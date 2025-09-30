package com.akra.geonsaehelperaiserver.domain.ai.model

import com.akra.geonsaehelperaiserver.domain.ai.config.AiProperties

data class AiChatRequest(
    val message: String,
    val systemPrompt: String? = null,
    val provider: AiProperties.Provider? = null
)

data class AiChatResponse(
    val content: String,
    val model: String,
    val provider: AiProperties.Provider
)
