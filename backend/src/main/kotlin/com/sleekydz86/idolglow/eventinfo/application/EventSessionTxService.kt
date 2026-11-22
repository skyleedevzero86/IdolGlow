package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSessionPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaChangeType
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaSnapshot
import com.sleekydz86.idolglow.eventinfo.domain.EventSchedule
import com.sleekydz86.idolglow.eventinfo.domain.EventSession
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.domain.KoreanPublicHolidays
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Service
class EventSessionTxService(
    private val eventSessionPort: EventSessionPort,
    private val eventMasterPort: EventMasterPort,
    private val eventMetaOutboxRecorder: EventMetaOutboxRecorder,
) {
    private val log = LoggerFactory.getLogger(EventSessionTxService::class.java)
    private val zone: ZoneId = IdolGlowDateTimes.ZONE

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun apply(
        sourceRefId: Long,
        guidance: String,
        today: LocalDate,
        horizon: LocalDate,
    ): EventSessionSyncResult {
        val ref = eventSessionPort.findRef(sourceRefId) ?: return EventSessionSyncResult.NONE
        val schedule = EventSchedule.parse(guidance)
        if (schedule.skipped.isNotEmpty()) {
            log.info(
                "공연 회차 안내 일부를 읽지 못했습니다. source={} sourceId={} skipped={}",
                ref.source,
                ref.sourceId,
                schedule.skipped,
            )
        }
        val result = reconcile(ref, desired(ref, schedule, today, horizon), today)
        ref.recordSchedule(guidance, horizon)
        if (result.changed) markChanged(ref)
        return result
    }

    private fun desired(
        ref: EventSourceRef,
        schedule: EventSchedule,
        today: LocalDate,
        horizon: LocalDate,
    ): Set<Instant> {
        val from = maxOf(today, ref.startDate ?: today)
        val to = minOf(horizon, ref.endDate ?: horizon)
        return schedule.expand(from, to, KoreanPublicHolidays::contains).map { it.atZone(zone).toInstant() }.toSet()
    }

    private fun reconcile(
        ref: EventSourceRef,
        desired: Set<Instant>,
        today: LocalDate,
    ): EventSessionSyncResult {
        val existing = eventSessionPort.findByRefFrom(ref.id, today.atStartOfDay(zone).toOffsetDateTime())
        val known = existing.map { it.startsAt.toInstant() }.toSet()
        val cancelled = existing.filter { it.startsAt.toInstant() !in desired }.count { it.cancel() }
        val revived = existing.filter { it.startsAt.toInstant() in desired }.count { it.reschedule() }
        val added = (desired - known).sorted()
        eventSessionPort.saveAll(added.map { newSession(ref, it) })
        val previous = ref.sessionHorizon?.plusDays(1)?.atStartOfDay(zone)?.toInstant()
        val addedInsideOld = if (previous == null) added.size else added.count { it < previous }
        return EventSessionSyncResult(added.size, cancelled, revived, cancelled + revived + addedInsideOld > 0)
    }

    private fun newSession(
        ref: EventSourceRef,
        startsAt: Instant,
    ): EventSession = EventSession(sourceRefId = ref.id, startsAt = startsAt.atZone(zone).toOffsetDateTime())

    private fun markChanged(ref: EventSourceRef) {
        val master = ref.confirmedGlobalId()?.let(eventMasterPort::findByGlobalId) ?: return
        master.markSessionsChanged(LocalDateTime.now())
        val fields = setOf(EventMetaSnapshot.FIELD_SESSIONS)
        eventMetaOutboxRecorder.record(master, EventMetaChangeType.SESSIONS_CHANGED, fields)
    }
}

data class EventSessionSyncResult(
    val added: Int,
    val cancelled: Int,
    val revived: Int,
    val changed: Boolean,
) {
    companion object {
        val NONE: EventSessionSyncResult = EventSessionSyncResult(0, 0, 0, false)
    }
}
