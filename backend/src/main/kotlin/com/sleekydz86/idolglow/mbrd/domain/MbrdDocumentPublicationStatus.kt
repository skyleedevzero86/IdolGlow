package com.sleekydz86.idolglow.mbrd.domain

import java.util.Locale

enum class MbrdDocumentPublicationStatus {
    DRAFT,
    PUBLISHED,
    ;

    fun toApiValue(): String = name.lowercase(Locale.ROOT)

    companion object {
        fun fromApiValue(value: String?): MbrdDocumentPublicationStatus {
            if (value.isNullOrBlank()) return PUBLISHED
            return when (value.trim().uppercase(Locale.ROOT)) {
                "DRAFT" -> DRAFT
                "PUBLISHED" -> PUBLISHED
                else -> throw IllegalArgumentException("지원하지 않는 문서 상태입니다: $value")
            }
        }
    }
}
