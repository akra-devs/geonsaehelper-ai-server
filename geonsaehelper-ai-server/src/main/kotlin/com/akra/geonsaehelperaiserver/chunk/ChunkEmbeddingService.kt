package com.akra.geonsaehelperaiserver.chunk

import com.akra.geonsaehelperaiserver.ai.config.AiProperties
import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingModel
import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingRequest
import com.akra.geonsaehelperaiserver.ai.service.AiEmbeddingService
import com.akra.geonsaehelperaiserver.vector.VectorDocumentPayload
import com.akra.geonsaehelperaiserver.vector.VectorStoreService
import com.akra.geonsaehelperaiserver.vector.VectorUpsertRequest
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ChunkEmbeddingService(
    private val semanticChunkService: SemanticChunkService,
    private val aiEmbeddingService: AiEmbeddingService,
    private val vectorStoreService: VectorStoreService
) {

    data class ChunkEmbedding(val chunk: String, val embedding: List<Float>)

    data class ChunkEmbeddingResult(
        val items: List<ChunkEmbedding>,
        val dimensions: Int,
        val model: String?,
        val provider: String?
    )

    fun chunkAndEmbed(
        text: String,
        options: SemanticChunkService.SemanticChunkOptions
    ): ChunkEmbeddingResult {
        val chunkResponse = semanticChunkService.chunkText(text, options)
//        val chunkResponse = ChunkResponse(text.chunked(1000))
        if (chunkResponse.content.isEmpty()) {
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
                model = AiEmbeddingModel.EMBEDDINGGEMMA_300M,
            )
        )

        val items = chunkResponse.content.zip(embeddingResponse.vectors) { chunk, vector ->
            ChunkEmbedding(chunk = chunk, embedding = vector)
        }

        val documents = items.mapIndexed { index, item ->
            VectorDocumentPayload(
                id = UUID.randomUUID().toString(),
                content = item.chunk,
                metadata = mapOf(
                    "chunk_index" to index,
                    "embedding_model" to embeddingResponse.model.value,
                    "provider" to embeddingResponse.provider.name
                )
            )
        }

        vectorStoreService.upsert(VectorUpsertRequest(documents))

        return ChunkEmbeddingResult(
            items = items,
            dimensions = embeddingResponse.dimensions,
            model = embeddingResponse.model.value,
            provider = embeddingResponse.provider.name
        )
    }
}
