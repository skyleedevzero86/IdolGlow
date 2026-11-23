package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSessionPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSession
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.OffsetDateTime

interface EventSessionJpaRepository : JpaRepository<EventSession, Long> {
    fun findBySourceRefIdAndStartsAtGreaterThanEqual(
        sourceRefId: Long,
        from: OffsetDateTime,
    ): List<EventSession>

    @Query(
        """
        select s from EventSession s, EventSourceRef r
        where s.sourceRefId = r.id and r.globalId = :globalId and r.matchStatus = :confirmed
          and s.startsAt >= :from and s.startsAt < :to
        order by s.startsAt, s.id
        """,
    )
    fun findByGlobalIdBetween(
        @Param("globalId") globalId: String,
        @Param("confirmed") confirmed: EventMatchStatus,
        @Param("from") from: OffsetDateTime,
        @Param("to") to: OffsetDateTime,
    ): List<EventSession>
}

interface EventScheduleTargetJpaRepository : JpaRepository<EventSourceRef, Long> {
    @Query(
        """
        select r from EventSourceRef r
        where r.source = :source and r.matchStatus = :confirmed and r.globalId is not null
          and (r.scheduleHash is null or r.scheduleHash <> r.rawHash)
        order by r.id
        """,
    )
    fun findFetchTargets(
        @Param("source") source: String,
        @Param("confirmed") confirmed: EventMatchStatus,
        pageable: Pageable,
    ): List<EventSourceRef>

    @Query(
        """
        select r from EventSourceRef r
        where r.source = :source and r.matchStatus = :confirmed and r.globalId is not null
          and r.scheduleHash = r.rawHash and r.scheduleGuidance is not null and r.scheduleGuidance <> ''
          and (r.sessionHorizon is null or r.sessionHorizon < :horizon)
        order by r.id
        """,
    )
    fun findRefreshTargets(
        @Param("source") source: String,
        @Param("confirmed") confirmed: EventMatchStatus,
        @Param("horizon") horizon: LocalDate,
        pageable: Pageable,
    ): List<EventSourceRef>
}

@Repository
class EventSessionAdapter(
    private val sessionRepository: EventSessionJpaRepository,
    private val targetRepository: EventScheduleTargetJpaRepository,
) : EventSessionPort {
    override fun saveAll(sessions: List<EventSession>): List<EventSession> =
        if (sessions.isEmpty()) emptyList() else sessionRepository.saveAll(sessions)

    override fun findByRefFrom(
        sourceRefId: Long,
        from: OffsetDateTime,
    ): List<EventSession> = sessionRepository.findBySourceRefIdAndStartsAtGreaterThanEqual(sourceRefId, from)

    override fun findByGlobalIdBetween(
        globalId: String,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<EventSession> = sessionRepository.findByGlobalIdBetween(globalId, EventMatchStatus.CONFIRMED, from, to)

    override fun findRef(sourceRefId: Long): EventSourceRef? = targetRepository.findById(sourceRefId).orElse(null)

    override fun findScheduleFetchTargets(
        source: String,
        limit: Int,
    ): List<EventSourceRef> =
        targetRepository.findFetchTargets(source, EventMatchStatus.CONFIRMED, PageRequest.of(0, limit))

    override fun findSessionRefreshTargets(
        source: String,
        horizon: LocalDate,
        limit: Int,
    ): List<EventSourceRef> =
        targetRepository.findRefreshTargets(source, EventMatchStatus.CONFIRMED, horizon, PageRequest.of(0, limit))
}
