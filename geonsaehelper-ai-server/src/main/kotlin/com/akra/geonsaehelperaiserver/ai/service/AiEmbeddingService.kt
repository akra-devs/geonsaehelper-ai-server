package com.akra.geonsaehelperaiserver.ai.service

import com.akra.geonsaehelperaiserver.ai.config.AiProperties
import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingModel
import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingRequest
import com.akra.geonsaehelperaiserver.ai.model.AiEmbeddingResponse
import org.springframework.ai.embedding.EmbeddingModel
import org.springframework.ai.embedding.EmbeddingOptionsBuilder
import org.springframework.ai.embedding.EmbeddingRequest
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class AiEmbeddingService(
    @Qualifier("ollamaEmbeddingModel")
    private val ollamaEmbeddingModel: EmbeddingModel,
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
        if (provider != AiProperties.Provider.OLLAMA) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Provider $provider is not enabled yet. Configure GPT support before use."
            )
        }

        val configuredModel = aiProperties.ollama.embeddingModel?.let { modelId ->
            AiEmbeddingModel.fromModelName(modelId) ?: throw ResponseStatusException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Configured embedding model '$modelId' is not supported"
            )
        }.let(::checkNotNull)

        val resolvedModel = request.model ?: configuredModel

        val optionsBuilder = EmbeddingOptionsBuilder.builder()
        optionsBuilder.withModel(resolvedModel.value)
        val embeddingRequest = EmbeddingRequest(normalizedInputs, optionsBuilder.build())

        val embeddingResponse = ollamaEmbeddingModel.call(embeddingRequest)
        val vectors = embeddingResponse.results.map { embedding -> embedding.output.toList() }
        val dimensions = vectors.firstOrNull()?.size ?: 0

        return AiEmbeddingResponse(
            vectors = vectors,
            dimensions = dimensions,
            model = resolvedModel,
            provider = provider
        )
    }
}
