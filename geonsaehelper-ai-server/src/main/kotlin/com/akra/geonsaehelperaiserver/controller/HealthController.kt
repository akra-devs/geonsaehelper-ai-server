package com.akra.geonsaehelperaiserver.controller

import org.springframework.beans.factory.ObjectProvider
import org.springframework.boot.info.BuildProperties
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.LinkedHashMap

@RestController
@RequestMapping("/api")
class HealthController(
    buildPropertiesProvider: ObjectProvider<BuildProperties>
) {

    private val buildProperties: BuildProperties? = buildPropertiesProvider.getIfAvailable()
    private val startedAt: Instant = Instant.now()

    @GetMapping("/health")
    fun health(): ResponseEntity<Map<String, Any>> {
        val now = Instant.now()
        val uptime = Duration.between(startedAt, now)

        val payload = LinkedHashMap<String, Any>(6)
        payload[KEY_STATUS] = STATUS_UP
        payload[KEY_VERSION] = buildProperties?.version ?: DEFAULT_VERSION
        payload[KEY_STARTED_AT] = formatInstant(startedAt)
        payload[KEY_UPTIME_SECONDS] = uptime.seconds
        payload[KEY_TIMESTAMP] = formatInstant(now)
        buildProperties?.name?.let { payload[KEY_APPLICATION] = it }

        return ResponseEntity.ok(payload)
    }

    private fun formatInstant(instant: Instant): String =
        formatter.format(instant.atOffset(ZoneOffset.UTC))

    private companion object {
        const val STATUS_UP = "UP"
        const val DEFAULT_VERSION = "unknown"
        const val KEY_STATUS = "status"
        const val KEY_VERSION = "version"
        const val KEY_STARTED_AT = "startedAt"
        const val KEY_UPTIME_SECONDS = "uptimeSeconds"
        const val KEY_TIMESTAMP = "timestamp"
        const val KEY_APPLICATION = "application"
        val formatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME
    }
}
