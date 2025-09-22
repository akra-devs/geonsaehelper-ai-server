package com.akra.geonsaehelperaiserver.ai.model

import com.akra.geonsaehelperaiserver.ai.config.AiProperties

enum class AiEmbeddingModel(val value: String) {
    EMBEDDINGGEMMA_300M("embeddinggemma:300m");

    companion object {
        fun fromModelName(id: String): AiEmbeddingModel? = entries.firstOrNull { it.value == id }
    }
}

data class AiEmbeddingRequest(
    val inputs: List<String>,
    val provider: AiProperties.Provider? = null,
    val model: AiEmbeddingModel? = null
)

data class AiEmbeddingResponse(
    val vectors: List<List<Float>>,
    val dimensions: Int,
    val model: AiEmbeddingModel,
    val provider: AiProperties.Provider
)
