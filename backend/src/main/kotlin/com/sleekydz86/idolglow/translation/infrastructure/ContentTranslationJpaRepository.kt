package com.sleekydz86.idolglow.translation.infrastructure

import com.sleekydz86.idolglow.translation.domain.ContentTranslation
import com.sleekydz86.idolglow.translation.domain.TranslationContentType
import com.sleekydz86.idolglow.translation.domain.TranslationStatus
import jakarta.persistence.LockModeType
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface ContentTranslationJpaRepository : JpaRepository<ContentTranslation, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select t from ContentTranslation t where t.id = :id")
    fun findByIdForUpdate(
        @Param("id") id: Long,
    ): ContentTranslation?

    fun findByContentTypeAndContentKey(
        contentType: TranslationContentType,
        contentKey: String,
    ): List<ContentTranslation>

    fun findByContentTypeAndContentKeyIn(
        contentType: TranslationContentType,
        contentKeys: Collection<String>,
    ): List<ContentTranslation>

    @Query(
        "select t from ContentTranslation t where t.status = :status " +
            "and (t.nextAttemptAt is null or t.nextAttemptAt <= :now) order by t.priority desc, t.id asc",
    )
    fun findDue(
        @Param("status") status: TranslationStatus,
        @Param("now") now: LocalDateTime,
        pageable: Pageable,
    ): List<ContentTranslation>

    fun findByContentTypeAndIdGreaterThanOrderByIdAsc(
        contentType: TranslationContentType,
        id: Long,
        pageable: Pageable,
    ): List<ContentTranslation>

    fun findByStatusOrderByIdDesc(
        status: TranslationStatus,
        pageable: Pageable,
    ): List<ContentTranslation>

    fun findByStatusAndContentTypeOrderByIdDesc(
        status: TranslationStatus,
        contentType: TranslationContentType,
        pageable: Pageable,
    ): List<ContentTranslation>

    fun countByStatus(status: TranslationStatus): Long

    fun countByStatusAndContentType(
        status: TranslationStatus,
        contentType: TranslationContentType,
    ): Long

    @Query(
        "select t.contentType as type, t.locale as locale, t.status as status, count(t) as total " +
            "from ContentTranslation t group by t.contentType, t.locale, t.status",
    )
    fun summarize(): List<TranslationSummaryRow>
}

interface TranslationSummaryRow {
    fun getType(): TranslationContentType

    fun getLocale(): String

    fun getStatus(): TranslationStatus

    fun getTotal(): Long
}
