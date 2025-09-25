package com.akra.geonsaehelperaiserver.chunk

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
        roleInstructions: String?,
        chunkSizeHint: Int?,
        maxChunkSize: Int?,
        mechanicalOverlap: Int?
    ): SemanticChunkService.SemanticChunkOptions {
        val defaults = SemanticChunkService.SemanticChunkOptions()
        return defaults.copy(
            roleInstructions = roleInstructions ?: defaults.roleInstructions,
            chunkSizeHint = chunkSizeHint ?: defaults.chunkSizeHint,
            maxChunkSize = maxChunkSize ?: defaults.maxChunkSize,
            mechanicalOverlap = mechanicalOverlap ?: defaults.mechanicalOverlap
        )
    }
}
