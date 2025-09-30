package com.akra.geonsaehelperaiserver.domain.ai.service

import com.akra.geonsaehelperaiserver.domain.ai.config.AiProperties
import com.akra.geonsaehelperaiserver.domain.ai.model.AiChatRequest
import com.akra.geonsaehelperaiserver.domain.ai.model.AiChatResponse
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AiChatService(
    @Qualifier("ollamaChatModel")
    private val ollamaChatModel: ChatModel,
    private val aiProperties: AiProperties
) {

    fun chat(request: AiChatRequest): AiChatResponse {
        val userMessage = request.message.trim()
        if (userMessage.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "message must not be blank")
        }

        val provider = resolveProvider(request.provider)
        val systemPrompt = resolveSystemPrompt(request.systemPrompt)
        val prompt = buildPrompt(systemPrompt, userMessage)
        val content = callChatModel(prompt)
        val model = resolveModel(provider)

        return AiChatResponse(
            content = content,
            model = model,
            provider = provider
        )
    }

    private fun resolveProvider(requested: AiProperties.Provider?): AiProperties.Provider {
        val provider = requested ?: aiProperties.defaultProvider
        if (provider != AiProperties.Provider.OLLAMA) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Provider $provider is not enabled yet. Configure GPT support before use."
            )
        }
        return provider
    }

    private fun resolveModel(provider: AiProperties.Provider): String {
        val configuredModel = aiProperties.settings(provider).model
        if (configuredModel.isBlank()) {
            throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Model for provider $provider is not configured"
            )
        }
        return configuredModel
    }

    private fun resolveSystemPrompt(customPrompt: String?): String {
        val prompt = (customPrompt ?: aiProperties.defaultSystemPrompt).trim()
        return prompt
    }

    private fun buildPrompt(systemPrompt: String, userMessage: String): Prompt {
        val messages = buildList {
            if (systemPrompt.isNotEmpty()) {
                add(SystemMessage(systemPrompt))
            }
            add(UserMessage(userMessage))
        }
        return Prompt(messages)
    }

    private fun callChatModel(prompt: Prompt): String {
        val response = ollamaChatModel.call(prompt)
        return response.results.joinToString(separator = "\n") { it.output?.text.orEmpty() }
    }
}
