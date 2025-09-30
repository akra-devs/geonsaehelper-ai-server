package com.akra.geonsaehelperaiserver.domain.chunk

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class SemanticChunkAfterNormalizerTest {

    @Test
    fun `removes backticks quotes and backslashes`() {
        val raw = "`\"Hello\\World\"`"
        val normalized = SemanticChunkAfterNormalizer.normalize(raw)
        assertThat(normalized).isEqualTo("HelloWorld")
    }

    @Test
    fun `returns trimmed result`() {
        val raw = "  `Example`  "
        val normalized = SemanticChunkAfterNormalizer.normalize(raw)
        assertThat(normalized).isEqualTo("Example")
    }

    @Test
    fun `handles empty input`() {
        val normalized = SemanticChunkAfterNormalizer.normalize("")
        assertThat(normalized).isEmpty()
    }
}
