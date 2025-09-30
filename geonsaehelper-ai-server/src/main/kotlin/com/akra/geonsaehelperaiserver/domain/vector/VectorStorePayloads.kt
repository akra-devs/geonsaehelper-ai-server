package com.akra.geonsaehelperaiserver.domain.vector

import com.fasterxml.jackson.annotation.JsonSubTypes
import com.fasterxml.jackson.annotation.JsonTypeInfo
import com.fasterxml.jackson.annotation.JsonTypeName

interface VectorPayload {
    val id: String?
    val content: String
    fun metadata(): Map<String, Any?>
}

data class VectorDocumentPayload(
    override val id: String? = null,
    override val content: String,
    val metadata: Map<String, Any?> = emptyMap()
) : VectorPayload {
    override fun metadata(): Map<String, Any?> = metadata
}

data class LoanProductVectorPayload(
    override val id: String? = null,
    override val content: String,
    val productType: LoanProductType,
    val chunkIndex: Int,
    val embeddingModel: String,
    val provider: String,
    val extraMetadata: Map<String, Any?> = emptyMap()
) : VectorPayload {

    override fun metadata(): Map<String, Any?> = buildMap {
        put(KEY_PRODUCT_TYPE, productType.code)
        put(KEY_CHUNK_INDEX, chunkIndex)
        put(KEY_EMBEDDING_MODEL, embeddingModel)
        put(KEY_PROVIDER, provider)
        extraMetadata.forEach { (key, value) ->
            if (value != null && key !in reservedKeys) {
                put(key, value)
            }
        }
    }

    companion object {
        const val KEY_PRODUCT_TYPE = "product_type"
        const val KEY_CHUNK_INDEX = "chunk_index"
        const val KEY_EMBEDDING_MODEL = "embedding_model"
        const val KEY_PROVIDER = "provider"
        private val reservedKeys = setOf(
            KEY_PRODUCT_TYPE,
            KEY_CHUNK_INDEX,
            KEY_EMBEDDING_MODEL,
            KEY_PROVIDER
        )
    }
}

data class VectorUpsertRequest(
    val documents: List<LoanProductVectorPayload>
)

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
