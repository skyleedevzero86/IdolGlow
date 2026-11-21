package com.sleekydz86.idolglow.translation.application.port.out

import com.sleekydz86.idolglow.translation.domain.ContentTranslation
import com.sleekydz86.idolglow.translation.domain.SourceContent
import com.sleekydz86.idolglow.translation.domain.SourcePage
import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import com.sleekydz86.idolglow.translation.domain.TranslationGlossaryTerm
import com.sleekydz86.idolglow.translation.domain.TranslationReport
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import java.time.LocalDateTime

interface TranslationSourcePort {
    fun load(
        type: TranslationContentType,
        key: String,
    ): SourceContent?

    fun page(
        type: TranslationContentType,
        offset: Int,
        limit: Int,
    ): SourcePage
}

interface ContentTranslationPort {
    fun save(translation: ContentTranslation): ContentTranslation

    fun findById(id: Long): ContentTranslation?

    fun findByIdForUpdate(id: Long): ContentTranslation?

    fun findByTarget(
        type: TranslationContentType,
        key: String,
    ): List<ContentTranslation>

    fun findByTargets(
        type: TranslationContentType,
        keys: Collection<String>,
    ): List<ContentTranslation>

    fun findDue(
        now: LocalDateTime,
        limit: Int,
    ): List<ContentTranslation>

    fun findPageByType(
        type: TranslationContentType,
        afterId: Long,
        limit: Int,
    ): List<ContentTranslation>

    fun findByStatus(
        status: TranslationStatus,
        type: TranslationContentType?,
        page: Int,
        size: Int,
    ): List<ContentTranslation>

    fun countByStatus(
        status: TranslationStatus,
        type: TranslationContentType?,
    ): Long

    fun summary(): List<TranslationCount>
}

data class TranslationCount(
    val type: TranslationContentType,
    val locale: String,
    val status: TranslationStatus,
    val count: Long,
)

interface TranslationGlossaryPort {
    fun findAll(): List<TranslationGlossaryTerm>

    fun save(term: TranslationGlossaryTerm): TranslationGlossaryTerm

    fun delete(id: Long): Boolean
}

interface TranslationReportPort {
    fun exists(
        translationId: Long,
        userId: Long,
    ): Boolean

    fun save(report: TranslationReport): TranslationReport
}

interface TranslationEnginePort {
    fun available(): Boolean

    fun translate(request: EngineRequest): EngineResponse?

    fun judge(request: JudgeRequest): Int?
}

data class EngineRequest(
    val locale: String,
    val fields: Map<String, String>,
    val glossary: List<TranslationGlossaryTerm>,
)

data class EngineResponse(
    val fields: Map<String, String>?,
    val model: String,
)

data class JudgeRequest(
    val locale: String,
    val source: Map<String, String>,
    val translated: Map<String, String>,
)
