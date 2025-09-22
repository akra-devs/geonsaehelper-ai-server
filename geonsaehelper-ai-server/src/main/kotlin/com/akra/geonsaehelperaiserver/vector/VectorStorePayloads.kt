package com.akra.geonsaehelperaiserver.vector

data class VectorDocumentPayload(
    val id: String? = null,
    val content: String,
    val metadata: Map<String, Any?> = emptyMap()
)

data class VectorUpsertRequest(
    val documents: List<VectorDocumentPayload>
)

data class VectorSearchRequest(
    val query: String,
    val topK: Int? = null
)

data class VectorDocumentResponse(
    val id: String?,
    val content: String,
    val score: Double? = null,
    val metadata: Map<String, Any?> = emptyMap()
)

data class VectorSearchResponse(
    val documents: List<VectorDocumentResponse>
)
