package com.akra.geonsaehelperaiserver.chunk

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
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
    @Qualifier("ollamaChatModel")
    private val chatModel: ChatModel,
    private val objectMapper: ObjectMapper
) {

    data class SemanticChunkOptions(
        val roleInstructions: String? = null,
        val chunkSizeHint: Int = 900,
        val maxChunkSize: Int = 1200,
        val mechanicalOverlap: Int = 300
    )

    private val chunkListType = object : TypeReference<List<String>>() {}

    fun chunkText(text: String, options: SemanticChunkOptions = SemanticChunkOptions()): ChunkResponse {
        val startMillis = System.currentTimeMillis()
        val normalized = MarkdownNormalizer.normalize(text)
        val afterNormalize = System.currentTimeMillis()
        println("[SemanticChunkService] normalization took ${afterNormalize - startMillis} ms")
        if (normalized.isEmpty()) {
            println("[SemanticChunkService] mechanical chunking skipped (empty input)")
            println("[SemanticChunkService] semantic chunking skipped (empty input)")
            return ChunkResponse(emptyList())
        }

        val mechanicalChunks = OverlappingTextChunker.chunk(
            normalized,
            chunkSize = options.maxChunkSize,
            overlap = options.mechanicalOverlap
        )
        val afterMechanical = System.currentTimeMillis()
        println("[SemanticChunkService] mechanical chunking took ${afterMechanical - afterNormalize} ms (chunks=${mechanicalChunks.size})")

        val semanticChunks = mutableListOf<String>()
        val semanticStart = System.currentTimeMillis()

        val results = mechanicalChunks.mapIndexed { blockIndex, block ->
            val systemPrompt = buildSystemPrompt(options)
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

            val tempOption = OllamaOptions.builder().numCtx(2048).build()
            val spec = ChatClient
                .create(chatModel)
                .prompt(prompt)
                .options(tempOption)

            val response = retry(times = 3) {
                val aiResponse = spec.call()
                aiResponse.entity(ChunkResponse::class.java) ?: ChunkResponse(emptyList())
            }

            println("semanticChunks : $blockIndex 완료")
            response
        }

        val semanticDuration = System.currentTimeMillis() - semanticStart
        println("[SemanticChunkService] semantic chunking took ${semanticDuration} ms (blocks=${mechanicalChunks.size}, chunks=${semanticChunks.size})")
        return ChunkResponse(content = results.flatMap { it.content })
    }

    private fun buildSystemPrompt(options: SemanticChunkOptions): String {
        val basePrompt = """""
            당신은 기계적으로 분할된 텍스트 Chunk(앞뒤에 overlap 포함)를 의미 Chunk로 다시 묶는 어시스턴트입니다.                                                                                                                
            규칙을 반드시 따르세요.                                                                                                                                                                                            
            1. 이 입력 Chunk는 전체 문서의 일부이며, 앞뒤에 중복되거나 잘린 문장이 있을 수 있습니다. Chunk의 시작·끝에서 문장이 중간에 끊어진 흔적이 보이면 해당 문장을 통째로 제거하고 완전한 문장부터 사용하세요.              
            2. Chunk 안에서 제목·소제목과 그에 해당하는 문단·목록·표를 한 의미 Chunk로 묶어 토픽 단위로 구성하세요. 짧은 목록이나 문단은 인접 내용과 결합해 하나의 의미 Chunk가 충분한 문맥(대략 세 문장 또는 chunk_size_hint 수  
            준)을 갖도록 하세요.                                                                                                                                                                                               
            3. 의미 Chunk는 입력 순서를 그대로 따르며, 새로운 문장을 만들거나 내용을 변형하지 마세요. 동일하거나 거의 같은 문장이 두 의미 Chunk에 중복되면 안 됩니다.                                                            
            4. chunk_size_hint와 max_chunk_size를 준수하되, 의미 단절이 생기지 않도록 필요하면 약간 조정하세요.                                                                                                                
            5. 결과는 JSON 배열 하나만 출력합니다. 각 요소는 문자열 형태의 의미 Chunk이며, 배열 밖 텍스트나 설명은 금지입니다.
 
            추가 지침:
            - 입력에 없는 문장을 새로 만들지 않습니다.
            - 의미 chunk 간에 중복되거나 거의 동일한 문장이 나타나지 않도록 합니다.
            - 표는 헤더와 관련 행이 분리되지 않게, 목록은 제목과 항목을 함께 유지합니다.

            예시 입력 chunk:
            "### 대출 대상\n요건 1. 무주택자\n요건 2. 소득 제한 없음"
            예시 출력:
            ["대출 대상\n요건 1. 무주택자\n요건 2. 소득 제한 없음"]

            겹침 정리 예시:
            이전 의미 chunk가 "…계속 진행되었습니다."로 끝나고 현재 chunk가 같은 문장으로 시작하면 그 문장을 삭제하고 다음 완전한 문장부터 작성하세요. 현재 chunk가 "…을 참고하"처럼 미완성 문장으로 끝나면 그 문장은 제외하세요.
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

data class ChunkResponse(
    val content: List<String>,
)

private fun <T> retry(times: Int = 3, block: () -> T): T {
    require(times > 0) { "times must be greater than 0" }

    var lastError: Throwable? = null
    repeat(times) { attempt ->
        try {
            return block()
        } catch (ex: Throwable) {
            lastError = ex
            if (attempt < times - 1) {
                println("[SemanticChunkService] attempt ${attempt + 1} failed: ${ex.message}. Retrying...")
            }
        }
    }

    throw lastError ?: IllegalStateException("Retry failed but no exception captured.")
}