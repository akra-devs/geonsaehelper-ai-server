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
import kotlin.text.Charsets

@RestController
@RequestMapping("/api/chunks")
class ChunkEmbeddingController(
    private val chunkEmbeddingService: ChunkEmbeddingService
) {

    data class ChunkEmbeddingResponse(
        val items: List<Item>,
        val dimensions: Int,
        val model: String?,
        val provider: String?
    ) {
        data class Item(
            val chunk: String,
            val embedding: List<Float>
        )
    }

    @PostMapping("/semantic/embed", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun chunkAndEmbed(
        @RequestPart("file") file: MultipartFile,
        @RequestParam("roleInstructions", required = false) roleInstructions: String?,
        @RequestParam("chunkSizeHint", required = false) chunkSizeHint: Int?,
        @RequestParam("maxChunkSize", required = false) maxChunkSize: Int?,
        @RequestParam("mechanicalOverlap", required = false) mechanicalOverlap: Int?
    ): ChunkEmbeddingResponse {
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

        val result = chunkEmbeddingService.chunkAndEmbed(rawText, options)

        return ChunkEmbeddingResponse(
            items = result.items.map { ChunkEmbeddingResponse.Item(it.chunk, it.embedding) },
            dimensions = result.dimensions,
            model = result.model,
            provider = result.provider
        )
    }
}
