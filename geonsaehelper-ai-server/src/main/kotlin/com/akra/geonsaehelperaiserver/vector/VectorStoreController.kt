package com.akra.geonsaehelperaiserver.vector

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/vectors")
class VectorStoreController(
    private val vectorStoreService: VectorStoreService
) {

    @PostMapping
    fun upsert(@RequestBody request: VectorUpsertRequest): ResponseEntity<Void> {
        vectorStoreService.upsert(request)
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build()
    }

    @PostMapping("/search")
    fun search(@RequestBody request: VectorSearchRequest): VectorSearchResponse {
        return vectorStoreService.search(request)
    }
}
