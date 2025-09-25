package com.akra.geonsaehelperaiserver.vector

import org.slf4j.LoggerFactory
import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class VectorStoreService(
    private val vectorStore: VectorStore,
    private val properties: VectorStoreProperties,
) {

    companion object {
        private val logger = LoggerFactory.getLogger(VectorStoreService::class.java)
    }

    init {
        logger.info("[VectorStoreService] Initialized with vector store: {}", vectorStore::class.java.name)
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

        val searchRequest = SearchRequest.builder()
            .query(request.query)
            .topK(topK)
            .build()

        logger.debug("[VectorStoreService] Executing search (topK={})", topK)

        val results = vectorStore.similaritySearch(searchRequest) ?: emptyList()

        val documents = results.map { document ->
            VectorDocumentResponse(
                id = document.id,
                content = document.text ?: "",
                score = document.score,
                metadata = document.metadata
            )
        }

        return VectorSearchResponse(documents)
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
