package com.akra.geonsaehelperaiserver.domain.chunk

import com.akra.geonsaehelperaiserver.domain.ai.config.AiProperties
import com.akra.geonsaehelperaiserver.domain.ai.model.AiEmbeddingModel
import com.akra.geonsaehelperaiserver.domain.ai.model.AiEmbeddingRequest
import com.akra.geonsaehelperaiserver.domain.ai.service.AiEmbeddingService
import com.akra.geonsaehelperaiserver.util.MarkdownNormalizer
import com.akra.geonsaehelperaiserver.domain.vector.LoanProductType
import com.akra.geonsaehelperaiserver.domain.vector.LoanProductVectorPayload
import com.akra.geonsaehelperaiserver.domain.vector.VectorStoreService
import com.akra.geonsaehelperaiserver.domain.vector.VectorUpsertRequest
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.UUID

@Service
class ChunkEmbeddingService(
    private val chunkPipelineService: ChunkPipelineService,
    private val mechanicalChunkService: MechanicalChunkService,
    private val aiEmbeddingService: AiEmbeddingService,
    private val vectorStoreService: VectorStoreService
) {

    fun chunkAndEmbed(
        text: String,
        options: ChunkPipelineOptions,
        productType: LoanProductType = LoanProductType.UNKNOWN
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
            provider = embeddingResponse.provider.name,
            productType = productType
        )

        return ChunkEmbeddingResult(
            items = items,
            dimensions = embeddingResponse.dimensions,
            model = embeddingResponse.model.value,
            provider = embeddingResponse.provider.name
        )
    }

    private fun semanticChunkOrFallback(
        text: String,
        options: ChunkPipelineOptions
    ): ChunkResponse {
        if (text.isEmpty()) {
            return ChunkResponse(emptyList())
        }

        // TODO: 의미 청킹 안정화 후 주석 해제 및 fallback 제거 예정입니다.
        val semanticChunks = chunkPipelineService.chunkText(text, options)
        if (semanticChunks.content.isNotEmpty()) {
            return semanticChunks
        }

        logger.debug("[ChunkEmbeddingService] Using mechanical chunk fallback")
        val normalized = MarkdownNormalizer.normalize(text)
        return mechanicalChunkService.chunk(
            normalized,
            options.mechanical
        )
    }

    private fun upsertVectorDocuments(
        items: List<ChunkEmbedding>,
        model: String,
        provider: String,
        productType: LoanProductType
    ) {
        val documents = items.mapIndexed { index, item ->
            LoanProductVectorPayload(
                id = UUID.randomUUID().toString(),
                content = item.chunk,
                productType = productType,
                chunkIndex = index,
                embeddingModel = model,
                provider = provider
            )
        }

        vectorStoreService.upsert(VectorUpsertRequest(documents))
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ChunkEmbeddingService::class.java)
    }
}
