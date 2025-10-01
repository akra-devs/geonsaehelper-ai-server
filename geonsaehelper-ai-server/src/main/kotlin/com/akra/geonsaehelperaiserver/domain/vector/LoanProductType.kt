package com.akra.geonsaehelperaiserver.domain.vector

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class LoanProductType(
    val description: String,
    private val aliases: Set<String> = emptySet()
) {
    RENT_STANDARD(
        description = "버팀목전세자금",
        aliases = setOf("GENERAL", "RENT_BUTTRESS_JEONSE")
    ),
    RENT_YOUTH_BEOTIMMOK(
        description = "청년전용 버팀목전세자금",
        aliases = setOf("YOUTH", "RENT_YOUTH_JEONSE")
    ),
    RENT_NEWLYWED(
        description = "신혼부부전용 전세자금",
        aliases = setOf("NEWLYWED")
    ),
    RENT_NEWBORN_SPECIAL(
        description = "신생아 특례 버팀목대출",
        aliases = setOf("신생아 특례 버팀목전세자금")
    ),
    RENT_RENEWAL_SUPPORT(
        description = "갱신만료 임차인 지원 버팀목전세자금"
    ),
    RENT_DAMAGES_STANDARD(
        description = "전세피해 임차인 버팀목전세자금"
    ),
    RENT_DAMAGES_PRIORITY(
        description = "전세사기피해자 최우선변제금 버팀목 전세자금 대출"
    ),
    REFINANCE_DAMAGES(
        description = "전세피해임차인대상 버팀목전세대출대환",
        aliases = setOf("RENT_JEONSE_DAMAGE_REFINANCE")
    ),
    RENT_VULNERABLE_MOVE(
        description = "주거취약계층 이주지원 버팀목전세자금",
        aliases = setOf("RENT_VULNERABLE_RELOCATION")
    ),
    RENT_YOUTH_MONTHLY(
        description = "청년전용 보증부월세대출",
        aliases = setOf("RENT_YOUTH_MONTHLY_SUPPORT")
    ),
    UNKNOWN(
        description = "미지정"
    );

    @JsonValue
    fun jsonValue(): String = name

    fun matches(raw: String): Boolean {
        if (raw.isBlank()) {
            return false
        }
        val candidate = raw.normalizeLoanProductKey()
        if (candidate.isEmpty()) {
            return false
        }
        if (name.equals(candidate, ignoreCase = true)) {
            return true
        }
        if (description.normalizeLoanProductKey().equals(candidate, ignoreCase = true)) {
            return true
        }
        return aliases.any { alias -> alias.normalizeLoanProductKey().equals(candidate, ignoreCase = true) }
    }

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(raw: String?): LoanProductType {
            if (raw.isNullOrBlank()) {
                return UNKNOWN
            }
            val normalized = raw.normalizeLoanProductKey()
            if (normalized.isEmpty()) {
                return UNKNOWN
            }
            return entries.firstOrNull { entry -> entry.matches(normalized) } ?: UNKNOWN
        }
    }
}

private fun String.normalizeLoanProductKey(): String {
    var value = trim()
    if (value.isEmpty()) {
        return value
    }

    if (value.contains('/')) {
        value = value.substringAfterLast('/')
    }

    if (value.contains('\\')) {
        value = value.substringAfterLast('\\')
    }

    if (value.startsWith("LoanProductType.", ignoreCase = true)) {
        value = value.substringAfterLast('.')
    }

    if (value.endsWith(".md", ignoreCase = true)) {
        value = value.dropLast(3)
    }

    return value.trim()
}
