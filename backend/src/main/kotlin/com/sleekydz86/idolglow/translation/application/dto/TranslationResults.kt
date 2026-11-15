package com.sleekydz86.idolglow.translation.application.dto

import com.sleekydz86.idolglow.translation.domain.TranslationContentType

data class TranslationTarget(
    val type: TranslationContentType,
    val key: String,
)

enum class LookupStatus {
    TRANSLATED,
    PENDING,
    ORIGINAL,
}

data class TranslationLookupResult(
    val type: TranslationContentType,
    val key: String,
    val status: LookupStatus,
    val requestedLocale: String,
    val resolvedLocale: String,
    val machine: Boolean,
    val translationId: Long?,
    val fields: Map<String, String>,
)

data class TranslationRowResult(
    val id: Long,
    val type: TranslationContentType,
    val key: String,
    val locale: String,
    val status: String,
    val origin: String?,
    val model: String?,
    val promptVersion: String?,
    val checkNotes: String?,
    val attempts: Int,
    val reportCount: Int,
    val fields: Map<String, String>,
    val source: Map<String, String>,
)

data class TranslationSummaryResult(
    val type: TranslationContentType,
    val locale: String,
    val status: String,
    val count: Long,
)

data class GlossaryTermResult(
    val id: Long,
    val sourceTerm: String,
    val locale: String,
    val targetTerm: String?,
    val mode: String,
)
