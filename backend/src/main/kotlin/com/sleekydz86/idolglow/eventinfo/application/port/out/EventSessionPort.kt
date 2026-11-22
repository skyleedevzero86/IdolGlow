package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.EventSession
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import java.time.LocalDate
import java.time.OffsetDateTime

interface EventSessionPort {
    fun saveAll(sessions: List<EventSession>): List<EventSession>

    fun findByRefFrom(
        sourceRefId: Long,
        from: OffsetDateTime,
    ): List<EventSession>

    fun findByGlobalIdBetween(
        globalId: String,
        from: OffsetDateTime,
        to: OffsetDateTime,
    ): List<EventSession>

    fun findRef(sourceRefId: Long): EventSourceRef?

    fun findScheduleFetchTargets(
        source: String,
        limit: Int,
    ): List<EventSourceRef>

    fun findSessionRefreshTargets(
        source: String,
        horizon: LocalDate,
        limit: Int,
    ): List<EventSourceRef>
}
