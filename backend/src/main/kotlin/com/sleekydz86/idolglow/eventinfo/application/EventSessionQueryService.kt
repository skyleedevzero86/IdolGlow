package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSessionPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.EventSessionStatus
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime

@Service
class EventSessionQueryService(
    private val eventSessionPort: EventSessionPort,
    private val eventSourceRefPort: EventSourceRefPort,
) {
    @Transactional(readOnly = true)
    fun sessions(
        globalId: String,
        from: LocalDate?,
        to: LocalDate?,
    ): List<EventSessionView> {
        val start = from ?: LocalDate.now(IdolGlowDateTimes.ZONE)
        val end = to ?: start.plusDays(EventSessionSyncService.WINDOW_DAYS)
        require(!end.isBefore(start)) { "회차 조회 종료일이 시작일보다 앞설 수 없습니다. from=$start to=$end" }
        require(!end.isAfter(start.plusDays(MAX_RANGE_DAYS))) { "회차는 한 번에 ${MAX_RANGE_DAYS}일까지만 조회할 수 있습니다." }
        return eventSessionPort
            .findByGlobalIdBetween(globalId.trim(), start.startOfDay(), end.plusDays(1).startOfDay())
            .map { EventSessionView(it.startsAt.atZoneSameInstant(ZONE).toOffsetDateTime(), it.status) }
    }

    @Transactional(readOnly = true)
    fun sessionsOfSource(
        source: String,
        sourceId: String,
        from: LocalDate?,
        to: LocalDate?,
    ): EventSessionsOfSource {
        val globalId = eventSourceRefPort.findBySourceAndSourceId(source.trim(), sourceId.trim())?.confirmedGlobalId()
        return EventSessionsOfSource(globalId, globalId?.let { sessions(it, from, to) }.orEmpty())
    }

    private fun LocalDate.startOfDay(): OffsetDateTime = atStartOfDay(ZONE).toOffsetDateTime()

    companion object {
        const val MAX_RANGE_DAYS: Long = 92
        private val ZONE = IdolGlowDateTimes.ZONE
    }
}

data class EventSessionView(
    val startsAt: OffsetDateTime,
    val status: EventSessionStatus,
)

data class EventSessionsOfSource(
    val globalId: String?,
    val sessions: List<EventSessionView>,
)
