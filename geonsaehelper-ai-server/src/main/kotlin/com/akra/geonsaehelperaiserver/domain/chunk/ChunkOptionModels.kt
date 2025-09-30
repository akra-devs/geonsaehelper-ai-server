package com.akra.geonsaehelperaiserver.domain.chunk

data class MechanicalChunkOptions(
    val chunkSize: Int = 1200,
    val overlap: Int = 300,
)

data class SemanticChunkOptions(
    val roleInstructions: String? = null,
    val chunkSizeHint: Int = 500,
    val maxChunkSize: Int = 1000,
)

data class ChunkPipelineOptions(
    val mechanical: MechanicalChunkOptions = MechanicalChunkOptions(),
    val semantic: SemanticChunkOptions = SemanticChunkOptions()
)
