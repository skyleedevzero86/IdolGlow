package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin
import com.sleekydz86.idolglow.eventinfo.domain.MissingTranslation
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.Repository
import org.springframework.data.repository.query.Param

interface EventTranslationCoverageJpaRepository : Repository<EventMaster, Long> {
    fun countByStatus(status: EventMasterStatus): Long

    @Query(
        """
        select count(m) from EventMaster m
        where m.status = :status
          and exists (
            select 1 from EventTranslation t
            where t.globalId = m.globalId and t.locale = :locale and t.origin in :origins
          )
        """,
    )
    fun countWithOrigin(
        @Param("status") status: EventMasterStatus,
        @Param("locale") locale: String,
        @Param("origins") origins: Collection<EventTranslationOrigin>,
    ): Long

    @Query(
        """
        select count(m) from EventMaster m
        where m.status = :status
          and exists (
            select 1 from EventTranslation t
            where t.globalId = m.globalId and t.locale = :locale and t.origin in :hidden
          )
          and not exists (
            select 1 from EventTranslation v
            where v.globalId = m.globalId and v.locale = :locale and v.origin in :visible
          )
        """,
    )
    fun countHiddenOnly(
        @Param("status") status: EventMasterStatus,
        @Param("locale") locale: String,
        @Param("hidden") hidden: Collection<EventTranslationOrigin>,
        @Param("visible") visible: Collection<EventTranslationOrigin>,
    ): Long

    @Query(
        """
        select count(m) from EventMaster m
        where m.status = :status
          and not exists (
            select 1 from EventTranslation t
            where t.globalId = m.globalId and t.locale = :locale
          )
        """,
    )
    fun countWithoutLocale(
        @Param("status") status: EventMasterStatus,
        @Param("locale") locale: String,
    ): Long

    @Query(
        """
        select new com.sleekydz86.idolglow.eventinfo.domain.MissingTranslation(m.globalId, m.title)
        from EventMaster m
        where m.status = :status
          and not exists (
            select 1 from EventTranslation t
            where t.globalId = m.globalId and t.locale = :locale
          )
        order by m.globalId
        """,
    )
    fun findWithoutLocale(
        @Param("status") status: EventMasterStatus,
        @Param("locale") locale: String,
        pageable: Pageable,
    ): List<MissingTranslation>
}
