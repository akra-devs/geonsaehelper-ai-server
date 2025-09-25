package com.akra.geonsaehelperaiserver.vector

import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingRequest
import com.akra.geonsaehelperaiserver.ai.service.AiEmbeddingService
import io.qdrant.client.QdrantClient
import io.qdrant.client.WithPayloadSelectorFactory
import io.qdrant.client.grpc.JsonWithInt
import io.qdrant.client.grpc.Points
import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.document.DocumentMetadata
import org.springframework.ai.model.EmbeddingUtils
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.ai.vectorstore.qdrant.QdrantVectorStore
import org.springframework.stereotype.Service
import java.util.Optional
import java.util.concurrent.ExecutionException

@Service
class VectorStoreService(
    private val vectorStore: VectorStore,
    private val properties: VectorStoreProperties,
    private val aiEmbeddingService: AiEmbeddingService,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(VectorStoreService::class.java)
        private const val SCORE_THRESHOLD = SearchRequest.SIMILARITY_THRESHOLD_ACCEPT_ALL.toFloat()
        private const val PAYLOAD_CONTENT_KEY = "doc_content"
    }

    init {
        logger.info(
            "[VectorStoreService] Initialized with vector store: {}",
            vectorStore::class.java.name
        )
    }

    fun upsert(request: VectorUpsertRequest) {
        if (request.documents.isEmpty()) {
            logger.debug("[VectorStoreService] Upsert skipped (no documents)")
            return
        }

        val documents = request.documents.map { payload ->
            val metadata = sanitizeMetadata(payload.metadata)
            createDocument(payload, metadata)
        }

        logger.debug("[VectorStoreService] Upserting {} documents", documents.size)
        vectorStore.add(documents)
        logger.debug("[VectorStoreService] Upsert complete")
    }

    fun search(request: VectorSearchRequest): VectorSearchResponse {
        val topK = request.topK?.takeIf { it > 0 } ?: properties.defaultTopK

        val documents = when (val query = request.query) {
            is VectorQuery.Text -> searchWithTextQuery(query, topK)
            is VectorQuery.Vector -> searchWithVectorQuery(query, topK)
        }

        val responses = documents.map { document ->
            VectorDocumentResponse(
                id = document.id,
                content = document.text ?: "",
                score = document.score,
                metadata = document.metadata
            )
        }

        return VectorSearchResponse(responses)
    }

    private fun searchWithTextQuery(query: VectorQuery.Text, topK: Int): List<Document> {
        val trimmed = query.text.trim()
        if (trimmed.isEmpty()) {
            logger.debug("[VectorStoreService] Search skipped (blank text query)")
            return emptyList()
        }

        val embedded = embedQuery(trimmed)
        if (embedded != null) {
            searchByVector(embedded, topK)?.let { return it }
            logger.debug("[VectorStoreService] Falling back to semantic search after vector attempt failed")
        }

        return similaritySearch(trimmed, topK)
    }

    private fun searchWithVectorQuery(query: VectorQuery.Vector, topK: Int): List<Document> {
        if (query.values.isEmpty()) {
            logger.debug("[VectorStoreService] Search skipped (empty vector query)")
            return emptyList()
        }

        return searchByVector(query.values, topK) ?: emptyList()
    }

    private fun similaritySearch(text: String, topK: Int): List<Document> {
        val searchRequest = SearchRequest.builder()
            .query(text)
            .topK(topK)
            .build()

        return vectorStore.similaritySearch(searchRequest) ?: emptyList()
    }

    private fun embedQuery(text: String): List<Float>? =
        try {
            val response = aiEmbeddingService.embed(AiEmbeddingRequest(inputs = listOf(text)))
            response.vectors.firstOrNull()
        } catch (ex: Exception) {
            logger.warn("[VectorStoreService] Failed to embed query: {}", ex.message)
            null
        }

    private fun searchByVector(vector: List<Float>, topK: Int): List<Document>? {
        val qdrantStore = vectorStore as? QdrantVectorStore ?: return null
        val qdrantClient = resolveQdrantClient(qdrantStore) ?: return null
        val collectionName = resolveCollectionName(qdrantStore) ?: return null

        val searchPoints = Points.SearchPoints.newBuilder()
            .setCollectionName(collectionName)
            .setLimit(topK.toLong())
            .setWithPayload(WithPayloadSelectorFactory.enable(true))
            .addAllVector(EmbeddingUtils.toList(vector.toFloatArray()))
            .setScoreThreshold(SCORE_THRESHOLD)
            .build()

        return try {
            val scoredPoints = qdrantClient.searchAsync(searchPoints).get()
            scoredPoints.map { mapScoredPoint(it) }
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
            logger.warn("[VectorStoreService] Vector search interrupted: {}", ex.message)
            null
        } catch (ex: ExecutionException) {
            logger.warn("[VectorStoreService] Vector search failed: {}", ex.cause?.message ?: ex.message)
            null
        } catch (ex: Exception) {
            logger.warn("[VectorStoreService] Vector search error: {}", ex.message)
            null
        }
    }

    private fun resolveQdrantClient(store: QdrantVectorStore): QdrantClient? {
        val optional: Optional<Any> = store.getNativeClient()
        return optional
            .filter { it is QdrantClient }
            .map { it as QdrantClient }
            .orElse(null)
    }

    private fun resolveCollectionName(store: QdrantVectorStore): String? =
        runCatching {
            val field = QdrantVectorStore::class.java.getDeclaredField("collectionName")
            field.isAccessible = true
            field.get(store) as? String
        }.onFailure {
            logger.warn("[VectorStoreService] Unable to resolve Qdrant collection name: {}", it.message)
        }.getOrNull()

    private fun mapScoredPoint(point: Points.ScoredPoint): Document {
        val id = when {
            point.id.hasUuid() -> point.id.uuid
            point.id.hasNum() -> point.id.num.toString()
            else -> ""
        }
        val metadata = convertPayload(point.payloadMap)
        val content = (metadata.remove(PAYLOAD_CONTENT_KEY) as? String).orEmpty()
        metadata[DocumentMetadata.DISTANCE.value()] = 1f - point.score

        return Document.builder()
            .id(id)
            .text(content)
            .metadata(metadata)
            .score(point.score.toDouble())
            .build()
    }

    private fun convertPayload(payload: Map<String, io.qdrant.client.grpc.JsonWithInt.Value>): MutableMap<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        payload.forEach { (key, value) ->
            val converted: Any? = when (value.kindCase) {
                io.qdrant.client.grpc.JsonWithInt.Value.KindCase.STRING_VALUE -> value.stringValue
                io.qdrant.client.grpc.JsonWithInt.Value.KindCase.INTEGER_VALUE -> value.integerValue
                io.qdrant.client.grpc.JsonWithInt.Value.KindCase.DOUBLE_VALUE -> value.doubleValue
                io.qdrant.client.grpc.JsonWithInt.Value.KindCase.BOOL_VALUE -> value.boolValue
                io.qdrant.client.grpc.JsonWithInt.Value.KindCase.NULL_VALUE -> null
                else -> value.toString()
            }
            result[key] = converted
        }
        return result
    }

    private fun sanitizeMetadata(source: Map<String, Any?>): MutableMap<String, Any> {
        val sanitized = mutableMapOf<String, Any>()
        source.forEach { (key, value) ->
            if (value != null) {
                sanitized[key] = value
            }
        }
        return sanitized
    }

    private fun createDocument(
        payload: VectorDocumentPayload,
        metadata: MutableMap<String, Any>
    ): Document {
        return if (payload.id != null) {
            Document(payload.id, payload.content, metadata)
        } else {
            Document(payload.content, metadata)
        }
    }
}
