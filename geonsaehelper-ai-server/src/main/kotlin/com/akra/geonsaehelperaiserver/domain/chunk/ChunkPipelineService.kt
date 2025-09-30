package com.akra.geonsaehelperaiserver.domain.chunk

import com.akra.geonsaehelperaiserver.util.MarkdownNormalizer
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class ChunkPipelineService(
    private val mechanicalChunkService: MechanicalChunkService,
    private val semanticChunkService: SemanticChunkService
) {

    fun chunkText(
        text: String,
        options: ChunkPipelineOptions = ChunkPipelineOptions()
    ): ChunkResponse {
        val normalizationStart = System.currentTimeMillis()
        val normalized = MarkdownNormalizer.normalize(text)
        val normalizationDuration = System.currentTimeMillis() - normalizationStart
        logger.debug("[ChunkPipelineService] normalization took {} ms", normalizationDuration)

        if (normalized.isEmpty()) {
            logger.debug("[ChunkPipelineService] skipping chunking for empty input")
            return ChunkResponse(emptyList())
        }

        val mechanicalResponse = mechanicalChunkService.chunk(
            normalized,
            options.mechanical
        )
        if (mechanicalResponse.content.isEmpty()) {
            logger.debug("[ChunkPipelineService] skipping semantic chunking (no mechanical blocks)")
            return mechanicalResponse
        }

        return semanticChunkService.chunk(mechanicalResponse.content, options.semantic)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(ChunkPipelineService::class.java)
    }
}
