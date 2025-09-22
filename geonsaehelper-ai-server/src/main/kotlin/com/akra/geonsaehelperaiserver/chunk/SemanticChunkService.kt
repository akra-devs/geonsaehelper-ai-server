package com.akra.geonsaehelperaiserver.chunk

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.prompt.Prompt
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

    fun chunkText(text: String, options: SemanticChunkOptions = SemanticChunkOptions()): List<String> {
        val startMillis = System.currentTimeMillis()
        val normalized = MarkdownNormalizer.normalize(text)
        val afterNormalize = System.currentTimeMillis()
        println("[SemanticChunkService] normalization took ${afterNormalize - startMillis} ms")
        if (normalized.isEmpty()) {
            println("[SemanticChunkService] mechanical chunking skipped (empty input)")
            println("[SemanticChunkService] semantic chunking skipped (empty input)")
            return emptyList()
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

        mechanicalChunks.forEachIndexed { blockIndex, block ->
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

            val response = chatModel.call(prompt)
            val content = response.results.joinToString(separator = "\n") { it.output?.text.orEmpty() }
                .ifBlank { throw IllegalStateException("Semantic chunker returned empty response") }

            val dtoChunks = runCatching {
                objectMapper.readValue(content, chunkListType)
            }.getOrElse { ex ->
                throw IllegalStateException("Failed to parse semantic chunks JSON", ex)
            }

            dtoChunks.forEach { dto ->
                val cleaned = dto.trim()
                if (cleaned.isNotEmpty()) {
                    semanticChunks += cleaned
                }
            }
        }

        val semanticDuration = System.currentTimeMillis() - semanticStart
        println("[SemanticChunkService] semantic chunking took ${semanticDuration} ms (blocks=${mechanicalChunks.size}, chunks=${semanticChunks.size})")

        return semanticChunks
    }

    private fun buildSystemPrompt(roleInstructions: String?): String {
        val basePrompt = """
            당신은 텍스트를 의미 단위로 분할하는 도우미입니다.
            - 출력은 JSON 배열 하나이며 각 요소는 텍스트 Chunk(String)입니다.
            - 청크는 입력 순서와 내용을 유지하고, 절대로 새로운 문장을 만들어내지 않습니다.
            - 기계적 분할에서 생긴 겹침 부분은 앞뒤 청크는 최소한만 조정하되, 그 외 원문 내용은 그대로 보존하세요.
            - 요청된 chunk_size_hint를 우선 적용하되 의미 단절을 피하기 위해 약간 짧거나 길어질 수 있습니다. max_chunk_size는 절대 초과하지 마세요.
            - 차이가 애매한 경계는 앞뒤 청크를 조금 잘라 명확하게 분리합니다.
            - JSON 외 다른 텍스트는 추가하지 마세요.
        """.trimIndent()

        return if (roleInstructions.isNullOrBlank()) {
            basePrompt
        } else {
            basePrompt + "\n추가 역할 지침: ${roleInstructions.trim()}"
        }
    }

}
