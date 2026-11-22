package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDateTime

interface EventMasterJpaRepository : JpaRepository<EventMaster, Long> {
    fun findByGlobalId(globalId: String): EventMaster?

    fun findByNormalizedTitleAndStatus(
        normalizedTitle: String,
        status: EventMasterStatus,
    ): List<EventMaster>

    @Query(
        """
        select m from EventMaster m
        where (m.updatedAt > :updatedAt or (m.updatedAt = :updatedAt and m.globalId > :globalId))
          and m.updatedAt <= :until
        order by m.updatedAt asc, m.globalId asc
        """,
    )
    fun findChangedAfter(
        @Param("updatedAt") updatedAt: LocalDateTime,
        @Param("globalId") globalId: String,
        @Param("until") until: LocalDateTime,
        pageable: Pageable,
    ): List<EventMaster>
}
