package com.akra.geonsaehelperaiserver.controller.ai

import com.akra.geonsaehelperaiserver.domain.ai.model.LoanAdvisorRequest
import com.akra.geonsaehelperaiserver.domain.ai.model.LoanAdvisorStreamEvent
import com.akra.geonsaehelperaiserver.domain.ai.service.LoanAdvisorService
import org.slf4j.LoggerFactory
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter
import reactor.core.Disposable
import reactor.core.scheduler.Schedulers

@RestController
@RequestMapping("/api/loan-advisor")
class LoanAdvisorController(
    private val loanAdvisorService: LoanAdvisorService
) {

    private val logger = LoggerFactory.getLogger(LoanAdvisorController::class.java)

    @PostMapping("/stream", produces = [MediaType.TEXT_EVENT_STREAM_VALUE])
    fun streamAnswer(@RequestBody request: LoanAdvisorRequest): SseEmitter {
        val emitter = SseEmitter(0L)

        val subscription: Disposable = loanAdvisorService.answerStream(request)
            .publishOn(Schedulers.boundedElastic())
            .subscribe(
                { event ->
                    try {
                        emitter.send(SseEmitter.event().name(event.type).data(event))
                    } catch (ex: Exception) {
                        logger.warn("[LoanAdvisorController] Failed to send SSE event: {}", ex.message)
                        emitter.completeWithError(ex)
                    }
                },
                { error ->
                    logger.warn("[LoanAdvisorController] Streaming terminated with error: {}", error.message)
                    emitter.completeWithError(error)
                },
                {
                    emitter.complete()
                }
            )

        emitter.onCompletion { subscription.dispose() }
        emitter.onTimeout {
            subscription.dispose()
            emitter.complete()
        }

        return emitter
    }
}
