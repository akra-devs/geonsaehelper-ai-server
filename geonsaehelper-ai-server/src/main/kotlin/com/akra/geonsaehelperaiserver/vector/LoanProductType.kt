package com.akra.geonsaehelperaiserver.vector

import com.fasterxml.jackson.annotation.JsonCreator
import com.fasterxml.jackson.annotation.JsonValue

enum class LoanProductType(@get:JsonValue val code: String) {
    GENERAL("GENERAL"),
    YOUTH("YOUTH"),
    NEWLYWED("NEWLYWED"),
    UNKNOWN("UNKNOWN");

    companion object {
        @JvmStatic
        @JsonCreator
        fun fromValue(raw: String?): LoanProductType {
            if (raw.isNullOrBlank()) {
                return UNKNOWN
            }
            val normalized = raw.trim()
            return entries.firstOrNull { entry ->
                entry.name.equals(normalized, ignoreCase = true) ||
                    entry.code.equals(normalized, ignoreCase = true)
            } ?: UNKNOWN
        }
    }
}
