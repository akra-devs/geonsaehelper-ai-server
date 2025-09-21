package com.akra.geonsaehelperaiserver.ai.service

import com.akra.geonsaehelperaiserver.ai.config.AiProperties
import com.akra.geonsaehelperaiserver.ai.model.AiChatRequest
import com.akra.geonsaehelperaiserver.ai.model.AiChatResponse
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import org.springframework.beans.factory.annotation.Qualifier

@Service
class AiChatService(
    @Qualifier("ollamaChatModel")
    private val ollamaChatModel: ChatModel,
    private val aiProperties: AiProperties
) {
    fun chat(request: AiChatRequest): AiChatResponse {
        val trimmedMessage = request.message.trim()
        if (trimmedMessage.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "message must not be blank")
        }

        val provider = request.provider ?: aiProperties.defaultProvider
        if (provider != AiProperties.Provider.OLLAMA) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Provider $provider is not enabled yet. Configure GPT support before use."
            )
        }

        val systemPrompt = (request.systemPrompt ?: aiProperties.defaultSystemPrompt).trim()
        val messages = buildList {
            if (systemPrompt.isNotEmpty()) {
                add(SystemMessage(systemPrompt))
            }
            add(UserMessage(trimmedMessage))
        }

        val prompt = Prompt(messages)
        val response = ollamaChatModel.call(prompt)
        val content = response.results.joinToString(separator = "\n") { it.output.text }

        return AiChatResponse(
            content = content,
            model = aiProperties.ollama.model,
            provider = provider
        )
    }
}
