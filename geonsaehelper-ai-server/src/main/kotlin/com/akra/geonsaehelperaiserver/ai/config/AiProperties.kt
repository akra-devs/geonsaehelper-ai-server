package com.akra.geonsaehelperaiserver.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

data class ProviderSettings(
    var model: String = "",
    var temperature: Double? = null,
    var topP: Double? = null,
    var embeddingModel: String? = null,
    var apiKey: String? = null
)

@ConfigurationProperties(prefix = "app.ai")
data class AiProperties(
    var defaultProvider: Provider = Provider.OLLAMA,
    var defaultSystemPrompt: String = "You are a helpful study assistant.",
    var ollama: ProviderSettings = ProviderSettings(model = "gemma3:12b", embeddingModel = "embeddinggemma:300m"),
    var openai: ProviderSettings = ProviderSettings(model = "gpt-5-mini", embeddingModel = "text-embedding-3-small")
) {
    enum class Provider {
        OLLAMA,
        OPENAI
    }
}
