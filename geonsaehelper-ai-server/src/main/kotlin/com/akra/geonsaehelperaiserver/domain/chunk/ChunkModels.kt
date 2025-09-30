package com.akra.geonsaehelperaiserver.domain.chunk

data class ChunkResponse(
    val content: List<String>
)

data class ChunkEmbedding(
    val chunk: String,
    val embedding: List<Float>
)

data class ChunkEmbeddingResult(
    val items: List<ChunkEmbedding>,
    val dimensions: Int,
    val model: String?,
    val provider: String?
)
