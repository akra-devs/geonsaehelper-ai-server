package com.akra.geonsaehelperaiserver.ai.service

import com.akra.geonsaehelperaiserver.ai.config.AiProperties
import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingModel
import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingRequest
import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingResponse
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingOptionsBuilder
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AiEmbeddingService(
    @Qualifier("ollamaEmbeddingModel")
    private val ollamaEmbeddingModel: EmbeddingModel,
    @Qualifier("openAiEmbeddingModel")
    private val openAiEmbeddingModelProvider: ObjectProvider<EmbeddingModel>,
    private val aiProperties: AiProperties
) {
    fun embed(request: AiEmbeddingRequest): AiEmbeddingResponse {
        if (request.inputs.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "inputs must not be empty")
        }
        // exception 던지지 말고 mapNotNull이나 filter처리
        val normalizedInputs = request.inputs.mapIndexed { index, value ->
            val trimmed = value.trim()
            if (trimmed.isEmpty()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "inputs[$index] must not be blank")
            }
            trimmed
        }

        val provider = request.provider ?: aiProperties.defaultProvider
        val resolvedModel = resolveModel(provider, request.model)
        val embeddingModel = resolveEmbeddingModel(provider)

        val optionsBuilder = EmbeddingOptionsBuilder.builder()
        optionsBuilder.withModel(resolvedModel.value)
        val embeddingRequest = EmbeddingRequest(normalizedInputs, optionsBuilder.build())

        val embeddingResponse = embeddingModel.call(embeddingRequest)
        val vectors = embeddingResponse.results.map { embedding -> embedding.output.toList() }
        val dimensions = vectors.firstOrNull()?.size ?: 0

        return AiEmbeddingResponse(
            vectors = vectors,
            dimensions = dimensions,
            model = resolvedModel,
            provider = provider
        )
    }

    private fun resolveModel(
        provider: AiProperties.Provider,
        requestedModel: AiEmbeddingModel?
    ): AiEmbeddingModel {
        val configuredModelId = when (provider) {
            AiProperties.Provider.OLLAMA -> aiProperties.ollama.embeddingModel
            AiProperties.Provider.OPENAI -> aiProperties.openai.embeddingModel
        }

        val configuredModel = configuredModelId?.let { modelId ->
            AiEmbeddingModel.fromModelName(modelId) ?: throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Configured embedding model '$modelId' is not supported"
            )
        } ?: when (provider) {
            AiProperties.Provider.OLLAMA -> AiEmbeddingModel.EMBEDDINGGEMMA_300M
            AiProperties.Provider.OPENAI -> AiEmbeddingModel.TEXT_EMBEDDING_3_SMALL
        }

        if (requestedModel != null && requestedModel.provider != provider) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Model ${requestedModel.name} cannot be used with provider $provider"
            )
        }

        return requestedModel ?: configuredModel
    }

    private fun resolveEmbeddingModel(provider: AiProperties.Provider): EmbeddingModel {
        return when (provider) {
            AiProperties.Provider.OLLAMA -> ollamaEmbeddingModel
            AiProperties.Provider.OPENAI -> openAiEmbeddingModelProvider.getIfAvailable()
                ?: throw ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "OpenAI embeddings are not configured. Enable spring.ai.openai and supply an API key."
                )
        }
    }
}
