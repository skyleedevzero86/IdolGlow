package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSessionPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.PerformanceScheduleSourcePort
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EventSessionSyncService(
    private val scheduleSources: List<PerformanceScheduleSourcePort>,
    private val eventSessionPort: EventSessionPort,
    private val eventSessionTxService: EventSessionTxService,
) {
    private val log = LoggerFactory.getLogger(EventSessionSyncService::class.java)

    fun sync(today: LocalDate = LocalDate.now(IdolGlowDateTimes.ZONE)) {
        val horizon = today.plusDays(WINDOW_DAYS)
        scheduleSources.forEach { source -> syncSource(source, today, horizon) }
    }

    private fun syncSource(
        source: PerformanceScheduleSourcePort,
        today: LocalDate,
        horizon: LocalDate,
    ) {
        val targets = eventSessionPort.findScheduleFetchTargets(source.source, FETCH_LIMIT)
        val fetched = targets.count { fetchOne(source, it, today, horizon) }
        val changedByWindow =
            eventSessionPort
                .findSessionRefreshTargets(source.source, horizon, REFRESH_LIMIT)
                .count { eventSessionTxService.apply(it.id, it.scheduleGuidance.orEmpty(), today, horizon).changed }
        log.info(
            "공연 회차 동기화. source={} targets={} fetched={} changedByWindow={}",
            source.source,
            targets.size,
            fetched,
            changedByWindow,
        )
    }

    private fun fetchOne(
        source: PerformanceScheduleSourcePort,
        ref: EventSourceRef,
        today: LocalDate,
        horizon: LocalDate,
    ): Boolean {
        val guidance =
            try {
                source.guidance(ref.sourceId)
            } catch (error: IllegalStateException) {
                log.warn(
                    "공연 회차 안내를 받지 못했습니다. source={} sourceId={} message={}",
                    ref.source,
                    ref.sourceId,
                    error.message,
                )
                return false
            }
        eventSessionTxService.apply(ref.id, guidance, today, horizon)
        return true
    }

    companion object {
        const val WINDOW_DAYS: Long = 90
        const val FETCH_LIMIT: Int = 200
        const val REFRESH_LIMIT: Int = 1000
    }
}
