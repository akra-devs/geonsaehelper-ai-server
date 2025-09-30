package com.akra.geonsaehelperaiserver.domain.chunk

object SemanticChunkResponseParser {
    private val languageHints = setOf("json", "javascript", "js")

    fun extractChunkArray(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) {
            return trimmed
        }

        val normalized = trimmed
            .replace("\\r\\n", "\n")
            .replace("\\n", "\n")
            .replace("\\\"", "\"")

        if (normalized.startsWith("```")) {
            val withoutFence = normalized.removePrefix("```")
            val closingIndex = withoutFence.lastIndexOf("```")
            val inner = if (closingIndex >= 0) {
                withoutFence.substring(0, closingIndex)
            } else {
                withoutFence
            }

            return removeLanguageHint(inner).trim()
        }

        if (normalized.startsWith("`") && normalized.endsWith("`") && normalized.length > 2) {
            return normalized.substring(1, normalized.length - 1).trim()
        }

        return normalized
    }

    private fun removeLanguageHint(content: String): String {
        val lines = content.lineSequence().toList()
        if (lines.isEmpty()) {
            return content.trim()
        }

        val firstNonBlankIndex = lines.indexOfFirst { it.isNotBlank() }
        if (firstNonBlankIndex == -1) {
            return ""
        }

        val firstLine = lines[firstNonBlankIndex].trim()
        if (languageHints.any { it.equals(firstLine, ignoreCase = true) }) {
            return lines
                .drop(firstNonBlankIndex + 1)
                .joinToString(separator = "\n")
                .trim()
        }

        return content.trim()
    }
}
