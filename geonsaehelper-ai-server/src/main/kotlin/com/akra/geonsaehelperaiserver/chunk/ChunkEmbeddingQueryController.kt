package com.akra.geonsaehelperaiserver.chunk

import com.akra.geonsaehelperaiserver.vector.VectorSearchRequest
import com.akra.geonsaehelperaiserver.vector.VectorStoreService
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ResponseStatusException

@RestController
@RequestMapping("/api/chunks/semantic")
class ChunkEmbeddingQueryController(
    private val vectorStoreService: VectorStoreService
) {

    data class QueryRequest(
        val query: String,
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
        if (request.query.isBlank()) {
            throw ResponseStatusException(HttpStatus.BAD_REQUEST, "query must not be blank")
        }

        val response = vectorStoreService.search(
            VectorSearchRequest(
                query = request.query,
                topK = request.topK
            )
        )

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
