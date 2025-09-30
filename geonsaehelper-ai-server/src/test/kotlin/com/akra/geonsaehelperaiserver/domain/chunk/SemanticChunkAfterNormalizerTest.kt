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

    @Test
    fun `removes markdown emphasis markers`() {
        val raw = "**중요** 내용과 __강조__ ~~제거~~ *테스트* _사례_"
        val normalized = SemanticChunkAfterNormalizer.normalize(raw)
        assertThat(normalized).isEqualTo("중요 내용과 강조 제거 테스트 사례")
    }

    @Test
    fun `strips excessive heading markers`() {
        val raw = "### **소제목**\n#### 세부 제목"
        val normalized = SemanticChunkAfterNormalizer.normalize(raw)
        assertThat(normalized).isEqualTo("## 소제목\n## 세부 제목")
    }

    @Test
    fun `collapses redundant whitespace and blank lines`() {
        val raw = "문장 하나.\n\n\n문장   둘.\n\n\n\n문장 셋."
        val normalized = SemanticChunkAfterNormalizer.normalize(raw)
        assertThat(normalized).isEqualTo("문장 하나.\n\n문장 둘.\n\n문장 셋.")
    }
}
