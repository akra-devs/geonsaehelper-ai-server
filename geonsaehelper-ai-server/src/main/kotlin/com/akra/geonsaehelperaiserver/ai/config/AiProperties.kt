package com.akra.geonsaehelperaiserver.ai.config

import org.springframework.boot.context.properties.ConfigurationProperties

data class ProviderSettings(
    var model: String = "",
    var temperature: Double? = null,
    var topP: Double? = null
)

@ConfigurationProperties(prefix = "app.ai")
data class AiProperties(
    var defaultProvider: Provider = Provider.OLLAMA,
    var defaultSystemPrompt: String = "You are a helpful study assistant.",
    var ollama: ProviderSettings = ProviderSettings(model = "gps-oss:20b"),
    var openai: ProviderSettings = ProviderSettings(model = "gpt-4o-mini")
) {
    enum class Provider {
        OLLAMA,
        OPENAI
    }
}
