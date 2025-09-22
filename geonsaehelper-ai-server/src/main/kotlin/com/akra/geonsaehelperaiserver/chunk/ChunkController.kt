package com.akra.geonsaehelperaiserver.chunk

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/chunks")
class ChunkController(
    private val semanticChunkService: SemanticChunkService
) {

    data class SemanticChunkResponse(
        val chunks: List<String>
    )

    @PostMapping("/semantic", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun chunkSemantically(
        @RequestPart("file") file: MultipartFile,
        @RequestParam("roleInstructions", required = false) roleInstructions: String?,
        @RequestParam("chunkSizeHint", required = false) chunkSizeHint: Int?,
        @RequestParam("maxChunkSize", required = false) maxChunkSize: Int?,
        @RequestParam("mechanicalOverlap", required = false) mechanicalOverlap: Int?
    ): SemanticChunkResponse {
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

        val defaults = SemanticChunkService.SemanticChunkOptions()
        val options = defaults.copy(
            roleInstructions = roleInstructions ?: defaults.roleInstructions,
            chunkSizeHint = chunkSizeHint ?: defaults.chunkSizeHint,
            maxChunkSize = maxChunkSize ?: defaults.maxChunkSize,
            mechanicalOverlap = mechanicalOverlap ?: defaults.mechanicalOverlap
        )

        val chunks = semanticChunkService.chunkText(rawText, options)
        return SemanticChunkResponse(chunks.content)
    }
}
