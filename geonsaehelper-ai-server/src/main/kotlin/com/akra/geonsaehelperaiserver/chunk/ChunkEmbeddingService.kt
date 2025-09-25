package com.akra.geonsaehelperaiserver.chunk

import com.akra.geonsaehelperaiserver.ai.config.AiProperties
import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingModel
import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingRequest
import com.akra.geonsaehelperaiserver.ai.service.AiEmbeddingService
import com.akra.geonsaehelperaiserver.vector.VectorDocumentPayload
import com.akra.geonsaehelperaiserver.vector.VectorStoreService
import com.akra.geonsaehelperaiserver.vector.VectorUpsertRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ChunkEmbeddingService(
    private val semanticChunkService: SemanticChunkService,
    private val aiEmbeddingService: AiEmbeddingService,
    private val vectorStoreService: VectorStoreService
) {

    fun chunkAndEmbed(
        text: String,
        options: SemanticChunkService.SemanticChunkOptions
    ): ChunkEmbeddingResult {
        val chunkResponse = semanticChunkOrFallback(text, options)
        if (chunkResponse.content.isEmpty()) {
            logger.debug("[ChunkEmbeddingService] Skipping embedding (no chunks produced)")
            return ChunkEmbeddingResult(
                items = emptyList(),
                dimensions = 0,
                model = null,
                provider = null
            )
        }

        val embeddingResponse = aiEmbeddingService.embed(
            AiEmbeddingRequest(
                inputs = chunkResponse.content,
                provider = AiProperties.Provider.OPENAI,
                model = AiEmbeddingModel.TEXT_EMBEDDING_3_SMALL
            )
        )

        val items = chunkResponse.content.zip(embeddingResponse.vectors) { chunk, vector ->
            ChunkEmbedding(chunk = chunk, embedding = vector)
        }

        upsertVectorDocuments(
            items = items,
            model = embeddingResponse.model.value,
            provider = embeddingResponse.provider.name
        )

        return ChunkEmbeddingResult(
            items = items,
            dimensions = embeddingResponse.dimensions,
            model = embeddingResponse.model.value,
            provider = embeddingResponse.provider.name
        )
    }

    @Suppress("UNUSED_PARAMETER")
    private fun semanticChunkOrFallback(
        text: String,
        options: SemanticChunkService.SemanticChunkOptions
    ): ChunkResponse {
        if (text.isEmpty()) {
            return ChunkResponse(emptyList())
        }

        // TODO: 의미 청킹 안정화 후 주석 해제 및 fallback 제거 예정입니다.
         val semanticChunks = semanticChunkService.chunkText(text, options)
         if (semanticChunks.content.isNotEmpty()) {
             return semanticChunks
         }

        logger.debug("[ChunkEmbeddingService] Using mechanical chunk fallback")
        return ChunkResponse(text.chunked(DEFAULT_CHUNK_SIZE))
    }

    private fun upsertVectorDocuments(
        items: List<ChunkEmbedding>,
        model: String,
        provider: String
    ) {
        val documents = items.mapIndexed { index, item ->
            VectorDocumentPayload(
                id = UUID.randomUUID().toString(),
                content = item.chunk,
                metadata = mapOf(
                    "chunk_index" to index,
                    "embedding_model" to model,
                    "provider" to provider
                )
            )
        }

        vectorStoreService.upsert(VectorUpsertRequest(documents))
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ChunkEmbeddingService::class.java)
        private const val DEFAULT_CHUNK_SIZE = 1000
    }
}
