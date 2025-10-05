package com.akra.geonsaehelperaiserver.config

import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary

@Configuration
class EmbeddingModelConfig(
    @Qualifier("openAiEmbeddingModel")
    private val openAiEmbeddingModel: EmbeddingModel
) {
    @Bean
    @Primary
    fun primaryEmbeddingModel(): EmbeddingModel = openAiEmbeddingModel
}
