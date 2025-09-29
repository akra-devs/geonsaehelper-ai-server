package com.akra.geonsaehelperaiserver.chunk

import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/chunks")
class ChunkEmbeddingController(
    private val chunkEmbeddingService: ChunkEmbeddingService
) {

    @PostMapping("/semantic/embed", consumes = [MediaType.MULTIPART_FORM_DATA_VALUE])
    fun chunkAndEmbed(
        @RequestPart("file") file: MultipartFile,
        @RequestParam("roleInstructions", required = false) roleInstructions: String?,
        @RequestParam("chunkSizeHint", required = false) chunkSizeHint: Int?,
        @RequestParam("maxChunkSize", required = false) maxChunkSize: Int?,
        @RequestParam("mechanicalOverlap", required = false) mechanicalOverlap: Int?,
        @RequestParam("productType", required = false) productType: String?
    ): ChunkEmbeddingResponse {
        val rawText = ChunkUploadRequestSupport.readMarkdownFile(file)
        val options = ChunkUploadRequestSupport.buildOptions(
            roleInstructions = roleInstructions,
            chunkSizeHint = chunkSizeHint,
            maxChunkSize = maxChunkSize,
            mechanicalOverlap = mechanicalOverlap
        )

        val result = chunkEmbeddingService.chunkAndEmbed(rawText, options, productType)

        return ChunkEmbeddingResponse(
            items = result.items.map { ChunkEmbeddingResponse.Item(it.chunk, it.embedding) },
            dimensions = result.dimensions,
            model = result.model,
            provider = result.provider
        )
    }
}
