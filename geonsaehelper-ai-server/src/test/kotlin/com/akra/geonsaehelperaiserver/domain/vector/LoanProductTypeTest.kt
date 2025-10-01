package com.akra.geonsaehelperaiserver.domain.vector

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class LoanProductTypeTest {

    @Test
    fun `fromValue matches canonical code`() {
        assertThat(LoanProductType.fromValue("RENT_STANDARD")).isEqualTo(LoanProductType.RENT_STANDARD)
    }

    @Test
    fun `fromValue matches legacy alias`() {
        assertThat(LoanProductType.fromValue("RENT_BUTTRESS_JEONSE")).isEqualTo(LoanProductType.RENT_STANDARD)
    }

    @Test
    fun `fromValue matches korean description`() {
        assertThat(LoanProductType.fromValue("청년전용 버팀목전세자금")).isEqualTo(LoanProductType.RENT_YOUTH_BEOTIMMOK)
    }

    @Test
    fun `fromValue matches document path`() {
        val path = "src/main/kotlin/com/akra/docs/HUG_POLICY_DOCS/청년전용 보증부월세대출.MD"
        assertThat(LoanProductType.fromValue(path)).isEqualTo(LoanProductType.RENT_YOUTH_MONTHLY)
    }

    @Test
    fun `fromValue returns unknown for blank`() {
        assertThat(LoanProductType.fromValue(" ")).isEqualTo(LoanProductType.UNKNOWN)
    }
}
