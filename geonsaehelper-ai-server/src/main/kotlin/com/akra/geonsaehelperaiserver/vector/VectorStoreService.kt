package com.akra.geonsaehelperaiserver.vector

import org.springframework.ai.document.Document
import org.springframework.ai.vectorstore.SearchRequest
import org.springframework.ai.vectorstore.VectorStore
import org.springframework.stereotype.Service

@Service
class VectorStoreService(
    private val vectorStore: VectorStore,
    private val properties: VectorStoreProperties,
) {

    fun upsert(request: VectorUpsertRequest) {
        if (request.documents.isEmpty()) {
            return
        }

        val documents = request.documents.map { payload ->
            val metadata = payload.metadata
                .filterValues { it != null }
                .mapValues { (_, value) -> value as Any }
                .toMutableMap()

            if (payload.id != null) {
                Document(payload.id, payload.content, metadata)
            } else {
                Document(payload.content, metadata)
            }
        }

        vectorStore.add(documents)
    }

    fun search(request: VectorSearchRequest): VectorSearchResponse {
        val topK = request.topK?.takeIf { it > 0 } ?: properties.defaultTopK

        val searchRequest = SearchRequest.builder()
            .query(request.query)
            .topK(topK)
            .build()

        val results = vectorStore.similaritySearch(searchRequest) ?: emptyList()

        return VectorSearchResponse(
            documents = results.map { document ->
                VectorDocumentResponse(
                    id = document.id,
                    content = document.text ?: "",
                    score = document.score,
                    metadata = document.metadata
                )
            }
        )
    }
}
