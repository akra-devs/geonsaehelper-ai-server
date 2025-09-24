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
            val systemPrompt = buildSystemPrompt(options.roleInstructions)
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

    private fun buildSystemPrompt(roleInstructions: String?): String {
        val basePrompt = """
            당신은 긴 텍스트를 의미 단위로 분할하는 전문 어시스턴트입니다.
            입력 블록은 기계적 분할 과정에서 일정한 overlap을 두고 잘린 결과이므로, 시작과 끝에 미완성 문장이나 중복된 문장이 포함될 수 있습니다.
            아래 절차를 순서대로 수행하세요.
            1. 블록 전체를 읽고 제목, 문단, 목록, 표 등 의미 경계를 식별합니다.
            2. 앞부분에서 overlap으로 인해 이미 전달된 문장·문단·제목이 보이면 삭제하고, 문장이 자연스럽게 시작되는 지점부터 작성하세요. 겹친 구절이 문장 중간에서 시작한다면 해당 문장을 통째로 제거하고 다음 완전한 문장부터 시작합니다.
            3. 자연스러운 의미 단위(문단, 문장 그룹, 목록 항목 등)를 기준으로 청크를 나눕니다. 문장을 중간에서 자르거나 의미를 끊지 말고, 한 청크 안에 하나의 주제나 논리를 온전히 담으세요.
            4. 각 청크는 벡터 임베딩에 적합하도록 충분한 맥락을 포함해야 합니다. 제목만 분리하지 말고, 제목과 그에 대응하는 설명·목록을 함께 묶으세요. 2문장 미만이거나 200자 미만처럼 매우 짧은 내용은 다음 문단과 결합해 더 큰 의미 단위로 만드세요.
            5. 블록의 끝에서도 overlap으로 이어질 미완성 문장이나 dangling 구절이 있으면 제거합니다. 다음 블록이 이어받을 수 있도록 완전한 문장에서 청크를 마무리하고, 필요하면 마지막 문장을 통째로 제외하세요.
            6. 완성된 청크들을 순서대로 하나의 JSON 배열에 담아 출력합니다. 배열 외의 텍스트, 설명, 주석은 절대 추가하지 마세요.

            추가 지침:
            - 입력에 존재하지 않는 문장이나 표현을 절대 새로 만들지 마세요.
            - 목록과 표는 관련 항목을 가능한 한 같은 청크에 담아 의미 단위가 깨지지 않도록 하세요. 표의 헤더와 데이터 행, 목록의 제목과 항목이 분리되지 않게 하세요.
            - 동일하거나 거의 같은 문장이 두 청크에 반복되지 않도록 주의하세요.
            - chunk_size_hint를 참고하되 의미 단절을 피하기 위해 약간 조정할 수 있습니다. max_chunk_size는 절대 초과하지 마세요.

            겹침 제거 예시:
            이전 청크가 "…계속 진행되었습니다."로 끝나고 이번 블록이 같은 문장으로 시작하면, 해당 문장 전체를 삭제한 뒤 다음 완전한 문장부터 청크를 작성하세요. 블록 끝이 "…를 참고하기"처럼 미완성이라면 그 문장을 제외하고 종료하세요.
        """.trimIndent()

        return if (roleInstructions.isNullOrBlank()) {
            basePrompt
        } else {
            basePrompt + "\n추가 역할 지침: ${roleInstructions.trim()}"
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
