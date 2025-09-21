package com.akra.geonsaehelperaiserver.ai.controller

import com.akra.geonsaehelperaiserver.ai.model.AiChatRequest
import com.akra.geonsaehelperaiserver.ai.model.AiChatResponse
import com.akra.geonsaehelperaiserver.ai.service.AiChatService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chat")
class AiChatController(
    private val aiChatService: AiChatService
) {
    @PostMapping
    fun chat(@RequestBody request: AiChatRequest): ResponseEntity<AiChatResponse> {
        val response = aiChatService.chat(request)
        return ResponseEntity.ok(response)
    }
}
