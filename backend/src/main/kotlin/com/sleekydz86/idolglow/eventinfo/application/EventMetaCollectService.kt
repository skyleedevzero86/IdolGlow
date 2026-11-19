package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.PerformanceSourceBatchPort
import com.sleekydz86.idolglow.eventinfo.domain.EventCollectStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class EventMetaCollectService(
    private val sources: List<PerformanceSourceBatchPort>,
    private val eventMetaTxService: EventMetaTxService,
    private val eventSourceRefPort: EventSourceRefPort,
) {
    private val log = LoggerFactory.getLogger(EventMetaCollectService::class.java)

    fun collect(
        from: LocalDate,
        to: LocalDate,
    ) {
        sources.forEach { source -> collectOne(source, from, to) }
        log.info(
            "공연 매칭 결과. confirmed={} candidate={} conflict={}",
            eventSourceRefPort.countByMatchStatus(EventMatchStatus.CONFIRMED),
            eventSourceRefPort.countByMatchStatus(EventMatchStatus.CANDIDATE),
            eventSourceRefPort.countByMatchStatus(EventMatchStatus.CONFLICT),
        )
    }

    private fun collectOne(
        source: PerformanceSourceBatchPort,
        from: LocalDate,
        to: LocalDate,
    ) {
        val saved = runCatching { saveSource(source, from, to) }
        val error = saved.exceptionOrNull()
        if (error == null) {
            return
        }
        log.error("공연 공급처 수집에 실패했습니다. source={}", source.source, error)
        eventMetaTxService.recordRun(source.source, EventCollectStatus.FAILED, 0, error.message)
    }

    private fun saveSource(
        source: PerformanceSourceBatchPort,
        from: LocalDate,
        to: LocalDate,
    ) {
        val batch = source.fetchBatch(from, to)
        val items = batch.items.distinctBy { it.sourceId }
        items.forEach(eventMetaTxService::upsert)
        val reason = batch.incompleteReason
        if (reason == null) {
            eventMetaTxService.recordRun(source.source, EventCollectStatus.SUCCESS, items.size, null)
            return
        }
        log.warn("공연 공급처 일부만 수집했습니다. source={} itemCount={} reason={}", source.source, items.size, reason)
        eventMetaTxService.recordRun(source.source, EventCollectStatus.PARTIAL, items.size, reason)
    }
}
