package com.akra.geonsaehelperaiserver.controller.chunk

import com.akra.geonsaehelperaiserver.domain.chunk.QueryVectorSearchService
import com.akra.geonsaehelperaiserver.domain.vector.VectorQuery
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/chunks/semantic")
class ChunkEmbeddingQueryController(
    private val queryVectorSearchService: QueryVectorSearchService
) {

    data class QueryRequest(
        val query: VectorQuery,
        val topK: Int? = null
    )

    data class QueryResponse(
        val matches: List<Match>
    ) {
        data class Match(
            val id: String?,
            val chunk: String,
            val score: Double?,
            val metadata: Map<String, Any?>
        )
    }

    @PostMapping("/search")
    fun search(@RequestBody request: QueryRequest): QueryResponse {
        val response = queryVectorSearchService.search(request.query, request.topK)

        val matches = response.documents.map { document ->
            QueryResponse.Match(
                id = document.id,
                chunk = document.content,
                score = document.score,
                metadata = document.metadata
            )
        }

        return QueryResponse(matches)
    }
}
