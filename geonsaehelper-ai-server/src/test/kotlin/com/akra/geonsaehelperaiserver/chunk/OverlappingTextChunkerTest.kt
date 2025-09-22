package com.akra.geonsaehelperaiserver.chunk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OverlappingTextChunkerTest {

    @Test
    fun `returns empty list for blank input`() {
        val result = OverlappingTextChunker.chunk("   \n\t  ")
        assertThat(result).isEmpty()
    }

    @Test
    fun `splits text with default sizes`() {
        val paragraph = buildString {
            repeat(1600) { append('A') }
            repeat(1600) { append('B') }
        }

        val chunks = OverlappingTextChunker.chunk(paragraph)

        assertThat(chunks).hasSize(4)
        assertThat(chunks[0]).hasSize(1200)
        assertThat(chunks[1]).hasSize(1200)
        assertThat(chunks[2]).hasSize(1200)
        assertThat(chunks[3]).hasSize(500)
        assertThat(chunks[0].takeLast(300)).isEqualTo(chunks[1].take(300))
        assertThat(chunks[1].takeLast(300)).isEqualTo(chunks[2].take(300))
    }

    @Test
    fun `caps overlap to remain below chunk size`() {
        val text = "0123456789"
        val chunks = OverlappingTextChunker.chunk(text, chunkSize = 3, overlap = 5)

        assertThat(chunks).hasSize(10)
        assertThat(chunks.first()).isEqualTo("012")
        assertThat(chunks[1]).isEqualTo("123")
        assertThat(chunks[0].takeLast(2)).isEqualTo(chunks[1].take(2))
    }

    @Test
    fun `handles custom chunk and overlap`() {
        val text = (1..30).joinToString(separator = " ") { "word$it" }
        val chunks = OverlappingTextChunker.chunk(text, chunkSize = 50, overlap = 20)

        assertThat(chunks).hasSizeGreaterThan(1)
        chunks.windowed(2).forEach { (first, second) ->
            assertThat(first.takeLast(20)).isEqualTo(second.take(20))
        }
    }
}
