package com.akra.geonsaehelperaiserver.chunk

import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/chunks")
class ChunkController(
    private val semanticChunkService: SemanticChunkService
) {

    data class SemanticChunkRequest(
        val text: String,
        val roleInstructions: String? = null,
        val chunkSizeHint: Int? = null,
        val maxChunkSize: Int? = null,
        val mechanicalOverlap: Int? = null
    )

    data class SemanticChunkResponse(
        val chunks: List<String>
    )

    @PostMapping("/semantic")
    fun chunkSemantically(@RequestBody request: SemanticChunkRequest): SemanticChunkResponse {
        val rawText = request.text.trim()
        if (rawText.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "text must not be blank")
        }

        val defaults = SemanticChunkService.SemanticChunkOptions()
        val options = SemanticChunkService.SemanticChunkOptions(
            roleInstructions = request.roleInstructions,
            chunkSizeHint = request.chunkSizeHint ?: defaults.chunkSizeHint,
            maxChunkSize = request.maxChunkSize ?: defaults.maxChunkSize,
            mechanicalOverlap = request.mechanicalOverlap ?: defaults.mechanicalOverlap
        )

        val chunks = semanticChunkService.chunkText(rawText, options)
        return SemanticChunkResponse(chunks)
    }
}
