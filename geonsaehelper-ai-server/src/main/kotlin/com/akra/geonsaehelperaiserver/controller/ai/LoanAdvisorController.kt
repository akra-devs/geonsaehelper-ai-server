package com.akra.geonsaehelperaiserver.controller.ai

import com.akra.geonsaehelperaiserver.domain.ai.model.LoanAdvisorRequest
import com.akra.geonsaehelperaiserver.domain.ai.model.LoanAdvisorStreamEvent
import com.akra.geonsaehelperaiserver.domain.ai.service.LoanAdvisorService
import org.springframework.http.MediaType.parseMediaType
import org.springframework.http.ResponseEntity
import org.springframework.http.codec.ServerSentEvent
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import reactor.core.publisher.Flux

@RestController
@RequestMapping("/api/loan-advisor")
class LoanAdvisorController(
    private val loanAdvisorService: LoanAdvisorService
) {

    @PostMapping("/stream", produces = [TEXT_EVENT_STREAM_UTF8_VALUE])
    fun streamAnswer(
        @RequestBody request: LoanAdvisorRequest
    ): ResponseEntity<Flux<ServerSentEvent<LoanAdvisorStreamEvent>>> {
        val stream = loanAdvisorService.answerStream(request)
            .map { event ->
                ServerSentEvent.builder(event)
                    .event(event.type)
                    .build()
            }

        return ResponseEntity
            .ok()
            .contentType(parseMediaType(TEXT_EVENT_STREAM_UTF8_VALUE))
            .body(stream)
    }

    private companion object {
        const val TEXT_EVENT_STREAM_UTF8_VALUE = "text/event-stream;charset=UTF-8"
    }
}
