package com.sleekydz86.idolglow.translation.infrastructure

import com.sleekydz86.idolglow.translation.application.port.out.ContentTranslationPort
import com.sleekydz86.idolglow.translation.application.port.out.TranslationCount
import com.sleekydz86.idolglow.translation.application.port.out.TranslationGlossaryPort
import com.sleekydz86.idolglow.translation.application.port.out.TranslationReportPort
import com.sleekydz86.idolglow.translation.domain.ContentTranslation
import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import com.sleekydz86.idolglow.translation.domain.TranslationGlossaryTerm
import com.sleekydz86.idolglow.translation.domain.TranslationReport
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import org.springframework.data.domain.PageRequest
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

interface TranslationGlossaryTermJpaRepository : JpaRepository<TranslationGlossaryTerm, Long>

interface TranslationReportJpaRepository : JpaRepository<TranslationReport, Long> {
    fun existsByTranslationIdAndUserId(
        translationId: Long,
        userId: Long,
    ): Boolean
}

@Repository
class ContentTranslationAdapter(
    private val repository: ContentTranslationJpaRepository,
) : ContentTranslationPort {
    override fun save(translation: ContentTranslation): ContentTranslation = repository.saveAndFlush(translation)

    override fun findById(id: Long): ContentTranslation? = repository.findById(id).orElse(null)

    override fun findByIdForUpdate(id: Long): ContentTranslation? = repository.findByIdForUpdate(id)

    override fun findByTarget(
        type: TranslationContentType,
        key: String,
    ): List<ContentTranslation> = repository.findByContentTypeAndContentKey(type, key)

    override fun findByTargets(
        type: TranslationContentType,
        keys: Collection<String>,
    ): List<ContentTranslation> =
        if (keys.isEmpty()) emptyList() else repository.findByContentTypeAndContentKeyIn(type, keys)

    override fun findDue(
        now: LocalDateTime,
        limit: Int,
    ): List<ContentTranslation> = repository.findDue(TranslationStatus.PENDING, now, PageRequest.of(0, limit))

    override fun findPageByType(
        type: TranslationContentType,
        afterId: Long,
        limit: Int,
    ): List<ContentTranslation> =
        repository.findByContentTypeAndIdGreaterThanOrderByIdAsc(type, afterId, PageRequest.of(0, limit))

    override fun findByStatus(
        status: TranslationStatus,
        type: TranslationContentType?,
        page: Int,
        size: Int,
    ): List<ContentTranslation> {
        val pageable = PageRequest.of(page, size)
        return if (type == null) {
            repository.findByStatusOrderByIdDesc(status, pageable)
        } else {
            repository.findByStatusAndContentTypeOrderByIdDesc(status, type, pageable)
        }
    }

    override fun countByStatus(
        status: TranslationStatus,
        type: TranslationContentType?,
    ): Long =
        if (type == null) repository.countByStatus(status) else repository.countByStatusAndContentType(status, type)

    override fun summary(): List<TranslationCount> =
        repository.summarize().map { TranslationCount(it.getType(), it.getLocale(), it.getStatus(), it.getTotal()) }
}

@Repository
class TranslationGlossaryAdapter(
    private val repository: TranslationGlossaryTermJpaRepository,
) : TranslationGlossaryPort {
    override fun findAll(): List<TranslationGlossaryTerm> = repository.findAll()

    override fun save(term: TranslationGlossaryTerm): TranslationGlossaryTerm = repository.saveAndFlush(term)

    override fun delete(id: Long): Boolean {
        if (!repository.existsById(id)) return false
        repository.deleteById(id)
        return true
    }
}

@Repository
class TranslationReportAdapter(
    private val repository: TranslationReportJpaRepository,
) : TranslationReportPort {
    override fun exists(
        translationId: Long,
        userId: Long,
    ): Boolean = repository.existsByTranslationIdAndUserId(translationId, userId)

    override fun save(report: TranslationReport): TranslationReport = repository.saveAndFlush(report)
}
