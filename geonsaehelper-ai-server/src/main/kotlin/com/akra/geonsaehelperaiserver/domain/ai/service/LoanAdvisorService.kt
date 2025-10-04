package com.akra.geonsaehelperaiserver.domain.ai.service

import com.akra.geonsaehelperaiserver.domain.ai.config.AiProperties
import com.akra.geonsaehelperaiserver.domain.ai.model.LoanAdvisorContext
import com.akra.geonsaehelperaiserver.domain.ai.model.LoanAdvisorRequest
import com.akra.geonsaehelperaiserver.domain.ai.model.LoanAdvisorStreamEvent
import com.akra.geonsaehelperaiserver.domain.vector.LoanProductVectorPayload
import com.akra.geonsaehelperaiserver.domain.vector.VectorDocumentResponse
import com.akra.geonsaehelperaiserver.domain.vector.VectorQuery
import com.akra.geonsaehelperaiserver.domain.vector.VectorSearchRequest
import com.akra.geonsaehelperaiserver.domain.vector.VectorStoreService
import org.slf4j.LoggerFactory
import org.springframework.ai.chat.messages.SystemMessage
import org.springframework.ai.chat.messages.UserMessage
import org.springframework.ai.chat.model.ChatModel
import org.springframework.ai.chat.model.StreamingChatModel
import org.springframework.ai.chat.prompt.Prompt
import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException
import reactor.core.publisher.Flux
import reactor.core.scheduler.Schedulers

fun interface LoanStreamingChatClient {
    fun stream(systemPrompt: String?, userPrompt: String): Flux<String>
}

interface LoanStreamingChatClientResolver {
    fun resolve(provider: AiProperties.Provider): LoanStreamingChatClient
}

@Service
class LoanAdvisorService(
    private val vectorStoreService: VectorStoreService,
    private val aiProperties: AiProperties,
    private val streamingChatClientResolver: LoanStreamingChatClientResolver
) {

    private val logger = LoggerFactory.getLogger(LoanAdvisorService::class.java)

    fun answerStream(request: LoanAdvisorRequest): Flux<LoanAdvisorStreamEvent> {
        return Flux.defer {
            val question = request.question.trim()
            if (question.isEmpty()) {
                throw ResponseStatusException(HttpStatus.BAD_REQUEST, "question must not be blank")
            }

            val provider = resolveProvider(request.provider)
            val productTypes = request.productTypes ?: throw IllegalArgumentException("productTypes can not be empty")
            val searchRequest = VectorSearchRequest(
                query = VectorQuery.Text(question),
                topK = request.topK,
                productTypes = productTypes
            )

            val searchResponse = vectorStoreService.search(searchRequest)
            val contexts = buildContexts(searchResponse.documents)
            val systemPrompt = buildSystemPrompt()
            val userPrompt = buildUserMessage(question, contexts)

            val contextEvent = LoanAdvisorStreamEvent.Context(contexts)
            val answerFlux = streamingChatClientResolver
                .resolve(provider)
                .stream(systemPrompt.takeIf { it.isNotEmpty() }, userPrompt)
                .filter { it.isNotBlank() }
                .map { chunk -> LoanAdvisorStreamEvent.AnswerChunk(chunk) }
                .doOnError { ex ->
                    logger.warn("[LoanAdvisorService] Streaming chat failed: {}", ex.message)
                }

            Flux.concat(
                Flux.just(contextEvent),
                answerFlux,
                Flux.just(LoanAdvisorStreamEvent.Complete())
            )
        }
            .subscribeOn(Schedulers.boundedElastic())
    }

    private fun resolveProvider(requested: AiProperties.Provider?): AiProperties.Provider {
        val provider = requested ?: aiProperties.defaultProvider
        if (provider != AiProperties.Provider.OLLAMA && provider != AiProperties.Provider.OPENAI) {
            throw ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Provider $provider is not supported"
            )
        }
        return provider
    }

    private fun buildContexts(documents: List<VectorDocumentResponse>): List<LoanAdvisorContext> =
        documents.mapIndexed { index, document ->
            LoanAdvisorContext(
                rank = index + 1,
                id = document.id,
                productType = document.metadata[LoanProductVectorPayload.KEY_PRODUCT_TYPE] as? String,
                productTypeDescription = document.metadata[LoanProductVectorPayload.KEY_PRODUCT_TYPE_DESCRIPTION] as? String,
                score = document.score,
                content = truncate(document.content)
            )
        }

    private fun buildSystemPrompt(): String {
        val base = aiProperties.defaultSystemPrompt.trim()
        val additional = """
        제공된 참고 자료만을 근거로 답변하세요. 자료에 근거가 없으면 추측하지 말고 '자료에서 확인되지 않습니다.'라고 응답하세요. 답변은 한국어로 작성하고, 필요한 경우 정책명을 명시하세요.
        """.trimIndent()

        return sequenceOf(base, additional)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString(separator = "\n\n")
    }

    private fun buildUserMessage(
        question: String,
        contexts: List<LoanAdvisorContext>
    ): String {
        val contextSection = if (contexts.isEmpty()) {
            "관련된 벡터 검색 결과가 없습니다. 자료가 없다고 명확히 밝히고 추가 정보를 요청하세요."
        } else {
            contexts.joinToString(separator = "\n\n") { context ->
                buildString {
                    appendLine("[자료 #${context.rank} | ${context.productTypeDescription ?: context.productType ?: "미분류"}]")
                    appendLine(context.content.trim())
                    context.score?.let { score ->
                        appendLine("(similarity_score=${"%.3f".format(score)})")
                    }
                }.trimEnd()
            }
        }

        return """
        아래는 질문과 연관된 참고 자료입니다.

        $contextSection

        사용자 질문: "$question"

        위 자료에 근거하여 질문에 답변하세요.
        """.trimIndent()
    }

    private fun truncate(content: String, maxLength: Int = 1200): String {
        val normalized = content.trim()
        if (normalized.length <= maxLength) {
            return normalized
        }
        return normalized.take(maxLength) + "…"
    }
}

@Component
class SpringAiStreamingChatClientResolver(
    @Qualifier("ollamaChatModel")
    private val ollamaChatModelProvider: ObjectProvider<ChatModel>,
    @Qualifier("ollamaStreamingChatModel")
    private val ollamaStreamingChatModelProvider: ObjectProvider<StreamingChatModel>,
    @Qualifier("openAiChatModel")
    private val openAiChatModelProvider: ObjectProvider<ChatModel>,
    @Qualifier("openAiStreamingChatModel")
    private val openAiStreamingChatModelProvider: ObjectProvider<StreamingChatModel>
) : LoanStreamingChatClientResolver {

    override fun resolve(provider: AiProperties.Provider): LoanStreamingChatClient {
        val chatModel = when (provider) {
            AiProperties.Provider.OLLAMA -> ollamaChatModelProvider.getIfAvailable()
            AiProperties.Provider.OPENAI -> openAiChatModelProvider.getIfAvailable()
        } ?: throw ResponseStatusException(
            HttpStatus.SERVICE_UNAVAILABLE,
            "Chat model for provider $provider is not configured"
        )

        val streamingChatModel = when (provider) {
            AiProperties.Provider.OLLAMA -> ollamaStreamingChatModelProvider.getIfAvailable()
            AiProperties.Provider.OPENAI -> openAiStreamingChatModelProvider.getIfAvailable()
        }

        return LoanStreamingChatClient { systemPrompt, userPrompt ->
            val messages = buildList {
                if (!systemPrompt.isNullOrBlank()) {
                    add(SystemMessage(systemPrompt))
                }
                add(UserMessage(userPrompt))
            }

            val prompt = Prompt(messages)

            val streamingFlux = streamingChatModel?.stream(prompt)
                ?.flatMap { response ->
                    val chunk = response.results
                        .mapNotNull { generation -> generation.output?.text }
                        .joinToString(separator = "")
                        .trim()

                    if (chunk.isBlank()) {
                        Flux.empty()
                    } else {
                        Flux.just(chunk)
                    }
                }

            streamingFlux ?: run {
                val response = chatModel.call(prompt)
                val content = response.results
                    .joinToString(separator = "\n") { generation ->
                        generation.output?.text.orEmpty()
                    }
                    .trim()

                Flux.fromIterable(splitIntoChunks(content))
            }
        }
    }

    private fun splitIntoChunks(text: String, maxLength: Int = 220): List<String> {
        if (text.isBlank()) {
            return emptyList()
        }

        val normalized = text.replace("\n\n", "\n").trim()
        val tokens = normalized.split(Regex("""\s+"""))
        if (tokens.isEmpty()) {
            return listOf(normalized)
        }

        val chunks = mutableListOf<String>()
        val builder = StringBuilder()
        tokens.forEach { token ->
            val addition = if (builder.isEmpty()) token else " $token"
            if (builder.length + addition.length > maxLength) {
                if (builder.isNotEmpty()) {
                    chunks.add(builder.toString())
                    builder.clear()
                }
                builder.append(token)
            } else {
                builder.append(addition)
            }
        }
        if (builder.isNotEmpty()) {
            chunks.add(builder.toString())
        }
        return chunks
    }
}
