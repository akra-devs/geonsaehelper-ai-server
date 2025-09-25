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
    @Qualifier("openAiEmbeddingModel")
    private val openAiEmbeddingModelProvider: ObjectProvider<EmbeddingModel>,
    private val aiProperties: AiProperties
) {

    fun embed(request: AiEmbeddingRequest): AiEmbeddingResponse {
        if (request.inputs.isEmpty()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "inputs must not be empty")
        }

        val normalizedInputs = normalizeInputs(request.inputs)
        val model = resolveModel(request)
        val embeddingModel = resolveEmbeddingModel()
        val embeddingRequest = buildEmbeddingRequest(normalizedInputs, model)

        val embeddingResponse = embeddingModel.call(embeddingRequest)
        val vectors = embeddingResponse.results.map { it.output.toList() }
        val dimensions = vectors.firstOrNull()?.size ?: 0

        return AiEmbeddingResponse(
            vectors = vectors,
            dimensions = dimensions,
            model = model,
            provider = AiProperties.Provider.OPENAI
        )
    }

    private fun normalizeInputs(inputs: List<String>): List<String> =
        inputs.mapNotNull { value ->
            val trimmed = value.trim()
            trimmed.ifEmpty { null }
        }

    private fun resolveEmbeddingModel(): EmbeddingModel =
        openAiEmbeddingModelProvider.getIfAvailable()
            ?: throw ResponseStatusException(
                HttpStatus.SERVICE_UNAVAILABLE,
                "OpenAI embeddings are not configured. Provide spring.ai.openai.api-key"
            )

    private fun buildEmbeddingRequest(
        inputs: List<String>,
        model: AiEmbeddingModel
    ): EmbeddingRequest {
        val options = EmbeddingOptionsBuilder.builder()
            .withModel(model.value)
            .build()
        return EmbeddingRequest(inputs, options)
    }

    private fun resolveModel(request: AiEmbeddingRequest): AiEmbeddingModel {
        val requestedModel = request.model

        if (request.provider != null && request.provider != AiProperties.Provider.OPENAI) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only OpenAI embeddings are supported now"
            )
        }

        if (requestedModel != null && requestedModel.provider != AiProperties.Provider.OPENAI) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Model ${requestedModel.value} is not supported. Use OpenAI embedding models."
            )
        }

        val configuredModel = configuredModel()
        val model = requestedModel ?: configuredModel

        if (model.provider != AiProperties.Provider.OPENAI) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Only OpenAI embedding models are supported"
            )
        }

        return model
    }

    private fun configuredModel(): AiEmbeddingModel {
        val configuredModelId = aiProperties.openai.embeddingModel

        return configuredModelId?.let { modelId ->
            AiEmbeddingModel.fromModelName(modelId) ?: throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Configured embedding model '$modelId' is not supported"
            )
        } ?: AiEmbeddingModel.TEXT_EMBEDDING_3_SMALL
    }
}
