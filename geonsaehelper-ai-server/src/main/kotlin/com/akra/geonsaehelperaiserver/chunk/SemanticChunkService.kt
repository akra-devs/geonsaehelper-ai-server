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
//                "block_index" to blockIndex,
//                "chunk_size_hint" to options.chunkSizeHint,
//                "max_chunk_size" to options.maxChunkSize,
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
//        val basePrompt = """
//            당신은 잘라진 텍스트 일부를 다시 의미 단위(임베딩 단위로써 벡터 DB에 의미있는 Chunk)로 chunk합니다. 입력 블록은 기계적 분할 이후 overlap이 남아 있어 앞뒤에 중복되거나 잘린 문장이 포함될 수 있습니다.
//            아래 지침을 그대로 따르세요.
//            0. 반드시 제공된 Chunk 단위를 준수하도록 하세요.
//            1. 블록을 빠르게 읽고 제목, 문단, 목록, 표 등 주요 주제를 파악합니다.
//            2. 블록의 앞·뒤에서 이전 Chunk와 겹칠 부분을 삭제합니다. 문장이 중간에서 시작되면 그 문장을 통째로 버리고 다음 완전한 문장부터, 끝부분도 동일하게 정리하세요.
//            3. 주제 단위로 Chunk를 만듭니다. 제목이나 소제목은 반드시 해당 설명·목록·표와 함께 같은 Chunk에 포함하세요. 목록 항목은 가능한 한 함께 유지하되 너무 길면 의미가 분리되는 지점에서만 나누세요.
//            4. 각 Chunk는 벡터 임베딩에 충분한 문맥을 제공해야 합니다. 세 문장 이상 또는 약 250자 이상이 되도록 인접 문단을 합치고, 너무 짧은 내용은 다음 문단과 결합하세요. chunk_size_hint를 참고하되 max_chunk_size는 절대 초과하지 마세요.
//            5. 완성된 Chunk들을 순서대로 하나의 JSON 배열에 담아 출력합니다. 배열 외 텍스트나 설명은 추가하지 마세요.
//
//            추가 지침:
//            - 입력에 없는 문장을 새로 만들지 마세요.
//            - 동일하거나 거의 같은 문장을 두 Chunk에 반복하지 마세요.
//            - 표는 헤더와 관련 행이 분리되지 않도록, 목록은 제목과 항목을 함께 두세요.
//
//            겹침 정리 예시:
//            이전 Chunk가 "…계속 진행되었습니다."로 끝나고 이번 블록이 같은 문장으로 시작하면 그 문장은 삭제하고 다음 완전한 문장부터 작성하세요. 블록 끝이 "…을 참고하"처럼 미완성이라면 그 문장을 제외하세요.
//        """.trimIndent()
        val basePrompt  = """
              당신은 겹침(overlap)이 남아 있는 텍스트 블록을 의미 단위 Chunk로 재구성하는 어시스턴트입니다.                                                                                                                      
              아래 규칙을 반드시 지키세요.                                                                                                                                                                                       
              1. 입력 블록을 읽어 제목·소제목·문단·목록·표 등 자연스러운 토픽 경계를 파악합니다.                                                                                                                                 
              2. 블록 앞뒤에서 이전/다음 Chunk와 겹칠 가능성이 있는 문장을 제거합니다. 문장이 중간에서 시작되면 그 문장 전체를 버리고 다음 완전한 문장부터 사용합니다. 끝부분도 동일하게 정리합니다.                             
              3. Chunk는 토픽 단위로 만듭니다. 제목(또는 소제목)은 반드시 해당 설명, 목록, 표와 같은 Chunk에 묶고, 목록 항목들은 가능한 한 함께 유지합니다.                                                                      
              4. Chunk마다 충분한 문맥을 포함해야 합니다. chunk_size_hint 수준으로 내용을 합치되 max_chunk_size는 넘기지 마세요. 아주 짧은 문단·목록·문장은 바로 뒤 내용과 결합해 하나의 덩어리를 만듭니다.                      
              5. 입력 문장을 그대로 사용하고 새로운 내용을 만들지 마세요. 동일하거나 거의 같은 문장이 두 Chunk에 반복되지 않도록 주의하세요.                                                                                     
              6. 결과는 JSON 배열 하나만 출력합니다. 배열 요소는 문자열 Chunk이며, 배열 밖에는 어떠한 텍스트도 포함하지 않습니다.         
        """.trimIndent()
        val sizeNotes = "\n하나의 Chunk 단위는=${options.chunkSizeHint}, 최대 Chunk 단위는=${options.maxChunkSize}을 반드시 준수 해주세요."

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
