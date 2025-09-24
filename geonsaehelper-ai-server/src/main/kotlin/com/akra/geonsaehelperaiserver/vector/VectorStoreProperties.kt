package com.akra.geonsaehelperaiserver.vector

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.ai.vectorstore")
data class VectorStoreProperties(
    val defaultTopK: Int = 4
)
