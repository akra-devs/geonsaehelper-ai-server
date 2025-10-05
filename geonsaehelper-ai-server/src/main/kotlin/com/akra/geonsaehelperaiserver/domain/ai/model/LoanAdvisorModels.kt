package com.akra.geonsaehelperaiserver.domain.ai.model

import com.akra.geonsaehelperaiserver.domain.ai.config.AiProperties
import com.akra.geonsaehelperaiserver.domain.vector.LoanProductType

data class LoanAdvisorRequest(
    val question: String,
    val userContext: Map<String, String>? = null,
    val productTypes: Set<LoanProductType>? = null,
    val topK: Int? = null,
    val provider: AiProperties.Provider? = null
)

data class LoanAdvisorContext(
    val rank: Int,
    val id: String?,
    val productType: String?,
    val productTypeDescription: String?,
    val score: Double?,
    val content: String
)

sealed interface LoanAdvisorStreamEvent {
    val type: String

    data class Context(
        val contexts: List<LoanAdvisorContext>,
        override val type: String = "context"
    ) : LoanAdvisorStreamEvent

    data class AnswerChunk(
        val content: String,
        override val type: String = "answer"
    ) : LoanAdvisorStreamEvent

    data class Complete(
        override val type: String = "complete"
    ) : LoanAdvisorStreamEvent
}
