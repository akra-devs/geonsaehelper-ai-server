package com.akra.geonsaehelperaiserver.chunk

data class SemanticChunkResponse(
    val chunks: List<String>
)

data class ChunkEmbeddingResponse(
    val items: List<Item>,
    val dimensions: Int,
    val model: String?,
    val provider: String?
) {
    data class Item(
        val chunk: String,
        val embedding: List<Float>
    )
}
