package com.akra.geonsaehelperaiserver.controller.ai

import com.akra.geonsaehelperaiserver.domain.ai.model.AiEmbeddingRequest
import com.akra.geonsaehelperaiserver.domain.ai.model.AiEmbeddingResponse
import com.akra.geonsaehelperaiserver.domain.ai.service.AiEmbeddingService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/embeddings/test")
class AiEmbeddingTestController(
    private val aiEmbeddingService: AiEmbeddingService
) {

    @GetMapping
    fun embedSample(): AiEmbeddingResponse {
        val request = AiEmbeddingRequest(
            inputs = SAMPLE_CHUNKS
        )
        return aiEmbeddingService.embed(request)
    }

    companion object {
        private val SAMPLE_CHUNKS = listOf(
            "HUG 주택청약 상품 요약",
            "중소기업 청년 전월세 보증부 대출: 최대 1.2억원까지 지원",
            "특례 보금자리론은 주택가격 9억 이하, 연소득 1억 이하 대상",
            "청년버팀목 전세대출 금리 연 2.2%~3.3% 구간"
        )
    }
}
