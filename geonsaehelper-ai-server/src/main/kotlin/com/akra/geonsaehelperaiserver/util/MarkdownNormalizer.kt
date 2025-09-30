package com.akra.geonsaehelperaiserver.util

/**
 * Lightweight normalizer to remove markup that bloats token counts before chunking.
 */
object MarkdownNormalizer {

    private val htmlTagRegex = "<[^>]+>".toRegex(setOf(RegexOption.DOT_MATCHES_ALL, RegexOption.IGNORE_CASE))
    private val collapsingWhitespaceRegex = "\\s+".toRegex()
    private val emphasisMarkers = listOf("**", "__", "~~", "*", "_")

    fun normalize(text: String): String {
        if (text.isBlank()) return text.trim()

        var normalized = htmlTagRegex.replace(text, "")
        emphasisMarkers.forEach { marker ->
            normalized = normalized.replace(marker, "")
        }

        normalized = collapsingWhitespaceRegex.replace(normalized, " ")

        return normalized.trim()
    }
}
