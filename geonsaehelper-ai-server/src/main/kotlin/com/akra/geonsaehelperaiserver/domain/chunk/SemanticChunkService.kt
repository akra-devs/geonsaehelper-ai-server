package com.akra.geonsaehelperaiserver.domain.chunk

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.client.ChatClient
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.ai.ollama.api.OllamaOptions
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.stereotype.Service

@Service
class SemanticChunkService(
    @Qualifier("openAiChatModel")
    private val chatModel: ChatModel,
    private val objectMapper: ObjectMapper
) {

    companion object {
        private val logger = LoggerFactory.getLogger(SemanticChunkService::class.java)
        private const val DEFAULT_NUM_CTX = 2048
    }

    fun chunk(
        blocks: List<String>,
        options: SemanticChunkOptions = SemanticChunkOptions()
    ): ChunkResponse {
        if (blocks.isEmpty()) {
            logger.debug("[SemanticChunkService] skipping semantic chunking for empty blocks")
            return ChunkResponse(emptyList())
        }

        val systemPrompt = buildSystemPrompt(options)
        val semanticStart = System.currentTimeMillis()

        val results = blocks.mapIndexed { blockIndex, block ->
            val userPayload = mapOf(
                "block_index" to blockIndex,
                "chunk_size_hint" to options.chunkSizeHint,
                "max_chunk_size" to options.maxChunkSize,
                "text" to block
            )
            val userMessage = objectMapper.writeValueAsString(userPayload)

            val prompt = Prompt(
                listOf(
                    SystemMessage(systemPrompt),
                    UserMessage(userMessage)
                )
            )

            val spec = ChatClient
                .create(chatModel)
                .prompt(prompt)
                .options(ollamaOptions())

            val response = retry(times = 3) {
                val aiResponse = spec.call()
                aiResponse.entity(ChunkResponse::class.java) ?: ChunkResponse(emptyList())
            }

            logger.debug(
                "[SemanticChunkService] semantic chunk {} completed (chunk_count={})",
                blockIndex,
                response.content.size
            )

            response
        }

        val semanticDuration = System.currentTimeMillis() - semanticStart
        logger.debug(
            "[SemanticChunkService] semantic chunking took {} ms (blocks={})",
            semanticDuration,
            blocks.size
        )

        val normalizedChunks = results
            .flatMap { it.content }
            .map { SemanticChunkAfterNormalizer.normalize(it) }
            .filter { it.isNotBlank() }

        return ChunkResponse(normalizedChunks)
    }

    private fun ollamaOptions(): OllamaOptions =
        OllamaOptions.builder().numCtx(DEFAULT_NUM_CTX).build()

    private fun <T> retry(times: Int = 3, block: () -> T): T {
        require(times > 0) { "times must be greater than 0" }

        var lastError: Throwable? = null
        repeat(times) { attempt ->
            try {
                return block()
            } catch (ex: Throwable) {
                lastError = ex
                if (attempt < times - 1) {
                    logger.warn(
                        "[SemanticChunkService] attempt {} failed: {}. Retrying...",
                        attempt + 1,
                        ex.message
                    )
                }
            }
        }

        throw lastError ?: IllegalStateException("Retry failed but no exception captured.")
    }

    private fun buildSystemPrompt(options: SemanticChunkOptions): String {
        val basePrompt = """
        당신은 "의미 단위 재묶기 어시스턴트"입니다. 입력은 원문에서 기계적으로 분할된 Chunk(앞뒤 overlap 포함)입니다. 목표는 문서의 "토픽(제목+그 내용 전체)" 기준으로 Chunk를 재구성하는 것입니다.
        
        [입력]
        - raw_chunk: 원문 일부(앞뒤가 잘려 있을 수 있음)
        - prev_tail: 이전 의미 Chunk의 끝부분 텍스트(겹침/중복 제거용, 없으면 빈 문자열)
        - 옵션:
          - chunk_size_hint={chunk_size_hint} (추천 길이)
          - max_chunk_size={max_chunk_size} (절대 상한)
        
        [하드 규칙]
        1) **제목 결속(Title binding)**: 제목/소제목은 반드시 그에 속한 문단·목록·표와 함께 하나의 의미 Chunk로 묶습니다. (제목과 본문을 분리 금지)
        2) **토픽 단위 분리**: 서로 다른 소제목이 등장하면 반드시 새로운 Chunk로 분리합니다. 여러 소제목을 한 Chunk에 합치지 마세요.
        3) **길이 조절**: 결과 Chunk는 chunk_size_hint의 0.7배 이상, 1.2배 이하 분량을 목표로 합니다.  
           - 너무 짧으면 같은 소제목 범위의 문장을 더 포함해 보완합니다.  
           - 너무 길면 소제목/표/목록 경계에서 잘라 Chunk를 나눕니다.
        4) **겹침/잘림 처리**
           - raw_chunk가 prev_tail과 겹치면 현재 쪽 중복 부분은 삭제 후 이어서 시작합니다.
           - 시작/끝이 잘린 문장은 통째로 버립니다. 단, 버려서 지나치게 짧아지면 같은 소제목 내 다음 완전 문장을 포함합니다.
        5) **중복 금지**: 동일하거나 거의 동일한 문장은 결과 배열에 중복 배치하지 않습니다.
        6) **표/목록 결속**: 표는 헤더+관련 행이 반드시 같은 Chunk에 있어야 합니다. 목록은 제목과 항목을 함께 유지합니다.
        7) **순서 보존**: 입력 순서를 반드시 유지합니다. 요약, 새로운 문장 생성, 의미 변형은 금지합니다.
        
        [출력 형식]
        - JSON 배열 하나만 출력합니다.
        - 각 요소는 문자열 형태의 "의미 Chunk"입니다.
        - 배열 밖 설명/메타데이터는 출력하지 않습니다.
        
        [체크리스트(내부 점검 후에만 출력) — 출력에는 포함하지 않음]
        - ( ) 각 Chunk가 "제목+내용" 단위인가?
        - ( ) 서로 다른 소제목이 한 Chunk에 섞이지 않았는가?
        - ( ) chunk_size_hint의 0.7배 이상, 1.2배 이하인가?
        - ( ) 표/목록이 제목과 분리되지 않았는가?
        - ( ) prev_tail과 겹침이 제거되었는가?
        - ( ) 동일 문장 중복이 없는가?
        
        [입력 예시]
        raw_chunk:
        "## 대출 조건 요약
        - 대출대상: 임차보증금 3억원 이하
        - 자격요건: 무주택자
        - 대출금리: 연 1.2%~3.0%
        - 대출한도: 2.4억원
        - 대출기간: 2년"
        
        prev_tail: ""
        
        [출력 예시]
        ["## 대출 조건 요약
        - 대출대상: 임차보증금 3억원 이하
        - 자격요건: 무주택자
        - 대출금리: 연 1.2%~3.0%
        - 대출한도: 2.4억원
        - 대출기간: 2년"]
        """.trimIndent()

        val sizeNotes = "\nchunk_size_hint=${options.chunkSizeHint}, max_chunk_size=${options.maxChunkSize}를 참고해 의미 chunk 길이를 조정하세요."

        val promptWithSizes = basePrompt + sizeNotes

        return if (options.roleInstructions.isNullOrBlank()) {
            promptWithSizes
        } else {
            promptWithSizes + "\n추가 역할 지침: ${options.roleInstructions.trim()}"
        }
    }
}
