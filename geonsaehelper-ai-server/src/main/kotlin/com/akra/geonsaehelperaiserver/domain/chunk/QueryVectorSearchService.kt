package com.akra.geonsaehelperaiserver.domain.chunk

import com.akra.geonsaehelperaiserver.domain.vector.VectorQuery
import com.akra.geonsaehelperaiserver.domain.vector.VectorSearchRequest
import com.akra.geonsaehelperaiserver.domain.vector.VectorSearchResponse
import com.akra.geonsaehelperaiserver.domain.vector.VectorStoreService
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class QueryVectorSearchService(
    private val vectorStoreService: VectorStoreService
) {

    fun search(query: VectorQuery, topK: Int? = null): VectorSearchResponse {
        validateQuery(query)
        return vectorStoreService.search(VectorSearchRequest(query = query, topK = topK))
    }

    private fun validateQuery(query: VectorQuery) {
        when (query) {
            is VectorQuery.Text -> {
                if (query.text.isBlank()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not be blank")
                }
            }

            is VectorQuery.Vector -> {
                if (query.values.isEmpty()) {
                    throw ResponseStatusException(HttpStatus.BAD_REQUEST, "vector must not be empty")
                }
            }
        }
    }
}
