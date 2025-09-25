package com.akra.geonsaehelperaiserver.vector

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

data class VectorDocumentPayload(
    val id: String? = null,
    val content: String,
    val metadata: Map<String, Any?> = emptyMap()
)

data class VectorUpsertRequest(
    val documents: List<VectorDocumentPayload>
)

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes(
    JsonSubTypes.Type(VectorQuery.Text::class, name = "text"),
    JsonSubTypes.Type(VectorQuery.Vector::class, name = "vector")
)
sealed interface VectorQuery {

    @JsonTypeName("text")
    data class Text(val text: String) : VectorQuery

    @JsonTypeName("vector")
    data class Vector(val values: List<Float>) : VectorQuery
}

data class VectorSearchRequest(
    val query: VectorQuery,
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
