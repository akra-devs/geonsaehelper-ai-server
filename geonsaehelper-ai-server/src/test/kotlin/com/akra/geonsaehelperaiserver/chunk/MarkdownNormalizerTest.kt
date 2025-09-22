package com.akra.geonsaehelperaiserver.chunk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class MarkdownNormalizerTest {

    @Test
    fun `removes html tags and emphasis markers`() {
        val input = "<p>**Hello** <span>__World__</span>~~!~~</p>"
        val result = MarkdownNormalizer.normalize(input)
        assertThat(result).isEqualTo("Hello World!")
    }

    @Test
    fun `collapses multiple whitespaces`() {
        val input = "Line1\n\n\nLine2    \tLine3"
        val result = MarkdownNormalizer.normalize(input)
        assertThat(result).isEqualTo("Line1 Line2 Line3")
    }
}
