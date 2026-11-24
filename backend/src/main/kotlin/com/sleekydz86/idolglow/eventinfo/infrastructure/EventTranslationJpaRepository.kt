package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventTitleTarget
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslation
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface EventTranslationJpaRepository : JpaRepository<EventTranslation, Long> {
    fun findByGlobalIdAndLocale(
        globalId: String,
        locale: String,
    ): EventTranslation?

    fun findByGlobalId(globalId: String): List<EventTranslation>

    fun findByGlobalIdIn(globalIds: Collection<String>): List<EventTranslation>

    @Query(
        """
        select new com.sleekydz86.idolglow.eventinfo.domain.EventTitleTarget(m.globalId, m.title)
        from EventMaster m
        left join EventTranslationAttempt a on a.globalId = m.globalId and a.locale = :locale
        where m.status = com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus.ACTIVE
          and m.startDate <= :to
          and coalesce(m.endDate, m.startDate) >= :from
          and exists (
            select r.id from EventSourceRef r
            where r.globalId = m.globalId
              and r.source = :source
              and r.matchStatus = com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus.CONFIRMED
          )
          and not exists (
            select t.id from EventTranslation t
            where t.globalId = m.globalId and t.locale = :locale
          )
        order by case when a.id is null then 0 else 1 end asc, a.lastFailedAt asc, m.startDate asc, m.globalId asc
        """,
    )
    fun findUntranslatedMasters(
        @Param("source") source: String,
        @Param("locale") locale: String,
        @Param("from") from: LocalDate,
        @Param("to") to: LocalDate,
        pageable: Pageable,
    ): List<EventTitleTarget>
}
