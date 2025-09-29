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
    var defaultSystemPrompt: String = "당신은 HUG의 전세 관련 assistant 입니다",
    var ollama: ProviderSettings = ProviderSettings(),
    var openai: ProviderSettings = ProviderSettings()
) {
    enum class Provider {
        OLLAMA,
        OPENAI
    }

    fun settings(provider: Provider): ProviderSettings =
        when (provider) {
            Provider.OLLAMA -> ollama
            Provider.OPENAI -> openai
        }
}
