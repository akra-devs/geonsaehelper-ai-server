package com.akra.geonsaehelperaiserver.domain.chunk

import org.springframework.http.HttpStatus
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException
import kotlin.text.Charsets

object ChunkUploadRequestSupport {

    fun readMarkdownFile(file: MultipartFile): String {
        if (file.isEmpty) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "file must not be empty")
        }

        val filename = file.originalFilename
        if (filename.isNullOrBlank() || !filename.lowercase().endsWith(".md")) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "file must have a .md extension")
        }

        val rawText = file.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        if (rawText.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "file must not contain only whitespace")
        }

        return rawText
    }

    fun buildOptions(
        chunkSizeHint: Int?,
        maxChunkSize: Int?,
        mechanicalOverlap: Int?
    ): ChunkPipelineOptions {
        val defaults = ChunkPipelineOptions()
        val resolvedMaxChunkSize = maxChunkSize ?: defaults.semantic.maxChunkSize
        val mechanicalOptions = defaults.mechanical.copy(
            chunkSize = resolvedMaxChunkSize,
            overlap = mechanicalOverlap ?: defaults.mechanical.overlap
        )
        val semanticOptions = defaults.semantic.copy(
            chunkSizeHint = chunkSizeHint ?: defaults.semantic.chunkSizeHint,
            maxChunkSize = resolvedMaxChunkSize
        )

        return ChunkPipelineOptions(
            mechanical = mechanicalOptions,
            semantic = semanticOptions
        )
    }
}
