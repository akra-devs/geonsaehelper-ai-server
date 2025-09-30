package com.akra.geonsaehelperaiserver.domain.chunk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SemanticChunkResponseParserTest {

    @Test
    fun `returns trimmed content when no fences`() {
        val json = "[\"one\", \"two\"]\n"
        val result = SemanticChunkResponseParser.extractChunkArray(json)
        assertThat(result).isEqualTo("[\"one\", \"two\"]")
    }

    @Test
    fun `strips triple backtick fence with language hint`() {
        val wrapped = """```json\n[\"one\", \"two\"]\n```"""
        val result = SemanticChunkResponseParser.extractChunkArray(wrapped)
        assertThat(result).isEqualTo("[\"one\", \"two\"]")
    }

    @Test
    fun `strips triple backtick fence without language hint`() {
        val wrapped = """```\n[\"one\"]\n```"""
        val result = SemanticChunkResponseParser.extractChunkArray(wrapped)
        assertThat(result).isEqualTo("[\"one\"]")
    }

    @Test
    fun `strips single backtick`() {
        val wrapped = "`[\"one\"]`"
        val result = SemanticChunkResponseParser.extractChunkArray(wrapped)
        assertThat(result).isEqualTo("[\"one\"]")
    }
}
