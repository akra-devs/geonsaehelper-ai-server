package com.akra.geonsaehelperaiserver.domain.chunk

object SemanticChunkAfterNormalizer {

    private val emphasisPatterns = listOf(
        Regex("\\*\\*(.+?)\\*\\*"),
        Regex("__(.+?)__"),
        Regex("~~(.+?)~~"),
        Regex("\\*(\\S(?:.*?\\S)?)\\*"),
        Regex("_(\\S(?:.*?\\S)?)_")
    )
    private val headingLevelThreeOrMore = Regex("^\\s*#{3,}\\s*")
    private val leadingDecorations = Regex("^\\s*[*_~]{2,}(?=\\s|$)")
    private val trailingDecorations = Regex("(?<=\\S)[*_~]{2,}$")
    private val innerWhitespace = Regex("(?<=\\S)[ \\t]{2,}(?=\\S)")

    fun normalize(raw: String): String {
        if (raw.isBlank()) {
            return raw.trim()
        }

        val normalizedNewlines = raw.replace("\r\n", "\n")
        val cleanedLines = normalizedNewlines
            .lineSequence()
            .map { cleanLine(it) }
            .toList()

        val collapsed = collapseBlankLines(cleanedLines)
        val joined = collapsed.joinToString(separator = "\n")
        val withoutBlacklistedChars = filterBlacklistedCharacters(joined)

        return withoutBlacklistedChars.trim()
    }

    private fun cleanLine(line: String): String {
        if (line.isBlank()) {
            return ""
        }

        var current = line.replace("\r", "").replace("\t", " ")

        val headingMatch = headingLevelThreeOrMore.find(current)
        if (headingMatch != null) {
            val rest = current.substring(headingMatch.range.last + 1).trimStart()
            current = if (rest.isEmpty()) {
                ""
            } else {
                "## $rest"
            }
        }
        emphasisPatterns.forEach { regex ->
            current = regex.replace(current) { it.groupValues[1] }
        }

        current = leadingDecorations.replace(current) { "" }
        current = trailingDecorations.replace(current) { "" }

        current = innerWhitespace.replace(current) { " " }

        return current.trimEnd()
    }

    private fun collapseBlankLines(lines: List<String>): List<String> {
        val trimmedLeading = lines.dropWhile { it.isBlank() }
        val trimmedTrailing = trimmedLeading.dropLastWhile { it.isBlank() }

        if (trimmedTrailing.isEmpty()) {
            return emptyList()
        }

        val result = mutableListOf<String>()
        var previousBlank = false

        trimmedTrailing.forEach { line ->
            if (line.isBlank()) {
                if (!previousBlank) {
                    result.add("")
                }
                previousBlank = true
            } else {
                result.add(line)
                previousBlank = false
            }
        }

        return result
    }

    private fun filterBlacklistedCharacters(text: String): String = buildString(text.length) {
        text.forEach { ch ->
            when (ch) {
                '`', '\\', '"' -> Unit
                else -> append(ch)
            }
        }
    }
}
