package com.akra.geonsaehelperaiserver.chunk

object SemanticChunkResponseParser {
    private val languageHints = setOf("json", "javascript", "js")

    fun extractChunkArray(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return trimmed
        }

        if (trimmed.startsWith("```")) {
            val withoutFence = trimmed.removePrefix("```")
            val closingIndex = withoutFence.lastIndexOf("```")
            val inner = if (closingIndex >= 0) {
                withoutFence.substring(0, closingIndex)
            } else {
                withoutFence
            }

            return removeLanguageHint(inner).trim()
        }

        if (trimmed.startsWith("`") && trimmed.endsWith("`") && trimmed.length > 2) {
            return trimmed.substring(1, trimmed.length - 1).trim()
        }

        return trimmed
    }

    private fun removeLanguageHint(content: String): String {
        val trimmed = content.trimStart()
        val firstNewline = trimmed.indexOf('\n')
        if (firstNewline == -1) {
            return trimmed
        }

        val firstLine = trimmed.substring(0, firstNewline).trim()
        val remainder = trimmed.substring(firstNewline + 1)

        return if (languageHints.any { it.equals(firstLine, ignoreCase = true) }) {
            remainder.trim()
        } else {
            trimmed
        }
    }
}
