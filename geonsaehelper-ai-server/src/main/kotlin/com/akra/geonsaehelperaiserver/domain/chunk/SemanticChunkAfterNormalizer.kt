package com.akra.geonsaehelperaiserver.domain.chunk

object SemanticChunkAfterNormalizer {
    fun normalize(raw: String): String {
        if (raw.isEmpty()) {
            return raw
        }

        return buildString(raw.length) {
            raw.forEach { ch ->
                when (ch) {
                    '`', '\\', '"' -> Unit
                    else -> append(ch)
                }
            }
        }.trim()
    }
}
