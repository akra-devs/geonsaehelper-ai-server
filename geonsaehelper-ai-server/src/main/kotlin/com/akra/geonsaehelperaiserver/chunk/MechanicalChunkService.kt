package com.akra.geonsaehelperaiserver.chunk

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class MechanicalChunkService {

    fun chunk(text: String, options: MechanicalChunkOptions = MechanicalChunkOptions()): ChunkResponse {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            logger.debug("[MechanicalChunkService] skipping chunking for empty input")
            return ChunkResponse(emptyList())
        }

        val start = System.currentTimeMillis()
        val chunks = OverlappingTextChunker.chunk(
            trimmed,
            chunkSize = options.chunkSize,
            overlap = options.overlap
        )
        val duration = System.currentTimeMillis() - start
        logger.debug(
            "[MechanicalChunkService] mechanical chunking took {} ms (chunks={})",
            duration,
            chunks.size
        )

        return ChunkResponse(chunks)
    }

    private companion object {
        private val logger = LoggerFactory.getLogger(MechanicalChunkService::class.java)
    }
}
