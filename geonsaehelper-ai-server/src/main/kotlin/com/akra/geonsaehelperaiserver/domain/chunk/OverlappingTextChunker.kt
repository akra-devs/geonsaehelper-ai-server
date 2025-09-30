package com.akra.geonsaehelperaiserver.domain.chunk

object OverlappingTextChunker {

    private const val DEFAULT_CHUNK_SIZE = 1200
    private const val DEFAULT_OVERLAP = 300

    /**
     * Splits [text] into chunks of [chunkSize] characters with [overlap] characters
     * of context shared between consecutive chunks. If the requested chunk size is
     * smaller than or equal to the overlap, the overlap is reduced to guarantee
     * forward progress.
     */
    fun chunk(
        text: String,
        chunkSize: Int = DEFAULT_CHUNK_SIZE,
        overlap: Int = DEFAULT_OVERLAP
    ): List<String> {
        val normalizedText = text.trim()
        if (normalizedText.isEmpty()) {
            return emptyList()
        }

        val chunks = mutableListOf<String>()
        val effectiveChunkSize = chunkSize.coerceAtLeast(1)
        val effectiveOverlap = overlap.coerceAtLeast(0).coerceAtMost(effectiveChunkSize - 1)
        val step = (effectiveChunkSize - effectiveOverlap).coerceAtLeast(1)

        var index = 0
        while (index < normalizedText.length) {
            val end = (index + effectiveChunkSize).coerceAtMost(normalizedText.length)
            chunks += normalizedText.substring(index, end)
            index += step
        }

        return chunks
    }
}
