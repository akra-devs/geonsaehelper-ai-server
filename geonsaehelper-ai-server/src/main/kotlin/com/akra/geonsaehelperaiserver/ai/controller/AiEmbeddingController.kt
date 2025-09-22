package com.akra.geonsaehelperaiserver.ai.controller

import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingRequest
import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingResponse
import com.akra.geonsaehelperaiserver.ai.service.AiEmbeddingService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/embeddings")
class AiEmbeddingController(
    private val aiEmbeddingService: AiEmbeddingService
) {
    @PostMapping
    fun embed(@RequestBody request: AiEmbeddingRequest): ResponseEntity<AiEmbeddingResponse> {
        val response = aiEmbeddingService.embed(request)
        return ResponseEntity.ok(response)
    }
}
