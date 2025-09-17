package com.akra.geonsaehelperaiserver

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.context.properties.ConfigurationPropertiesScan
import org.springframework.boot.runApplication

@SpringBootApplication
@ConfigurationPropertiesScan
class GeonsaehelperAiServerApplication

fun main(args: Array<String>) {
    runApplication<GeonsaehelperAiServerApplication>(*args)
}
