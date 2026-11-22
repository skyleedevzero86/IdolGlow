package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.util.concurrent.atomic.AtomicLong

@Component
class EventMatchReviewMetrics(
    private val eventSourceRefPort: EventSourceRefPort,
    meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(EventMatchReviewMetrics::class.java)
    private val pending: Map<EventMatchStatus, AtomicLong> = REVIEW_STATUSES.associateWith { AtomicLong() }

    init {
        pending.forEach { (status, value) ->
            Gauge
                .builder(PENDING_METRIC, value) { it.get().toDouble() }
                .tag("status", status.name.lowercase())
                .strongReference(true)
                .register(meterRegistry)
        }
    }

    @Scheduled(
        initialDelayString = "\${eventinfo.match-review.initial-delay-ms:60000}",
        fixedDelayString = "\${eventinfo.match-review.refresh-ms:600000}",
    )
    fun refresh() {
        val counts = runCatching { REVIEW_STATUSES.associateWith(eventSourceRefPort::countByMatchStatus) }
        counts.exceptionOrNull()?.let { error ->
            log.warn("공연 매칭 검토 대기 지표 갱신에 실패했습니다.", error)
            return
        }
        counts.getOrThrow().forEach { (status, count) -> pending[status]?.set(count) }
    }

    fun pending(status: EventMatchStatus): Long? = pending[status]?.get()

    companion object {
        const val PENDING_METRIC: String = "idolglow.event.match.pending"
        private val REVIEW_STATUSES = listOf(EventMatchStatus.CANDIDATE, EventMatchStatus.CONFLICT)
    }
}
