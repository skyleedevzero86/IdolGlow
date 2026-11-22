package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMetaProjectionPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaApplyResult
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaProjection
import io.micrometer.core.instrument.Counter
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class EventMetaProjectionService(
    private val consumedEventGuard: ConsumedEventGuard,
    private val eventMetaProjectionPort: EventMetaProjectionPort,
    meterRegistry: MeterRegistry,
) {
    private val log = LoggerFactory.getLogger(javaClass)
    private val staleDropped: Counter =
        Counter
            .builder("event_meta_projection_stale_dropped")
            .description("메타 버전이 이미 반영된 것보다 낮아 버린 공연 메타 이벤트 수")
            .register(meterRegistry)

    @Transactional
    fun apply(message: EventMetaMessage): EventMetaApplyResult {
        val consumed =
            consumedEventGuard.firstDelivery(CONSUMER, message.eventId) ?: return EventMetaApplyResult.DUPLICATE
        val result = project(message)
        consumed.markResult(result)
        return result
    }

    private fun project(message: EventMetaMessage): EventMetaApplyResult {
        val snapshot = requireNotNull(message.snapshot) { "공연 메타 이벤트에 스냅샷이 없습니다. eventId=${message.eventId}" }
        val current = eventMetaProjectionPort.findByGlobalId(message.globalId)
        return when {
            current == null -> {
                eventMetaProjectionPort.save(EventMetaProjection.from(message.globalId, message.metaVersion, snapshot))
                EventMetaApplyResult.APPLIED
            }
            current.isNewerThanOrSame(message.metaVersion) -> {
                dropStale(current, message)
                EventMetaApplyResult.STALE
            }
            else -> {
                current.apply(message.metaVersion, snapshot)
                EventMetaApplyResult.APPLIED
            }
        }
    }

    private fun dropStale(
        current: EventMetaProjection,
        message: EventMetaMessage,
    ) {
        staleDropped.increment()
        log.info(
            "이미 더 새 버전이 반영돼 이벤트를 버립니다. globalId={} applied={} received={}",
            message.globalId,
            current.metaVersion,
            message.metaVersion,
        )
    }

    companion object {
        const val CONSUMER: String = "event-meta-projection"
    }
}
