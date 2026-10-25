package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventCollectLogPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.CollectedPerformance
import com.sleekydz86.idolglow.eventinfo.domain.EventCollectLog
import com.sleekydz86.idolglow.eventinfo.domain.EventCollectStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchDecision
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventPerformanceHasher
import com.sleekydz86.idolglow.eventinfo.domain.EventPerformanceMatcher
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.domain.EventTitleNormalizer
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class EventMetaTxService(
    private val eventMasterPort: EventMasterPort,
    private val eventSourceRefPort: EventSourceRefPort,
    private val eventCollectLogPort: EventCollectLogPort,
) {
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun upsert(item: CollectedPerformance) {
        val normalizedTitle = EventTitleNormalizer.normalize(item.title)
        require(normalizedTitle.isNotEmpty()) { "공연 제목을 정규화하면 비어 있습니다. title=${item.title}" }
        val hash = EventPerformanceHasher.hash(item)
        val existing = eventSourceRefPort.findBySourceAndSourceId(item.source, item.sourceId)
        if (existing != null && existing.matchStatus == EventMatchStatus.CONFIRMED && existing.rawHash == hash) {
            return
        }
        val decision = EventPerformanceMatcher.decide(item, eventMasterPort.findByNormalizedTitle(normalizedTitle))
        val resolved = resolve(item, existing, decision)
        val ref = existing ?: EventSourceRef.create(item.source, item.sourceId)
        ref.record(resolved.globalId, resolved.status, resolved.score, item, hash, LocalDateTime.now())
        eventSourceRefPort.save(ref)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun recordRun(
        source: String,
        status: EventCollectStatus,
        itemCount: Int,
        errorMessage: String?,
    ) {
        eventCollectLogPort.save(
            EventCollectLog.record(
                source = source,
                status = status,
                itemCount = itemCount,
                errorMessage = errorMessage,
                collectedAt = LocalDateTime.now(),
            ),
        )
    }

    private fun resolve(
        item: CollectedPerformance,
        existing: EventSourceRef?,
        decision: EventMatchDecision,
    ): ResolvedMatch =
        when (decision) {
            is EventMatchDecision.Attach -> ResolvedMatch(decision.globalId, decision.status, decision.score)
            is EventMatchDecision.Hold -> ResolvedMatch(null, decision.status, decision.score)
            EventMatchDecision.Fresh -> freshMaster(item, existing)
        }

    private fun freshMaster(
        item: CollectedPerformance,
        existing: EventSourceRef?,
    ): ResolvedMatch {
        val ownedGlobalId = soleConfirmedGlobalId(existing)
        if (ownedGlobalId != null) {
            eventMasterPort.findByGlobalId(ownedGlobalId)?.refresh(item)
            return ResolvedMatch(ownedGlobalId, EventMatchStatus.CONFIRMED, EventPerformanceMatcher.CONFIRMED_SCORE)
        }
        val created = eventMasterPort.save(EventMaster.open(item))
        return ResolvedMatch(created.globalId, EventMatchStatus.CONFIRMED, EventPerformanceMatcher.CONFIRMED_SCORE)
    }

    private fun soleConfirmedGlobalId(existing: EventSourceRef?): String? {
        val globalId = existing?.takeIf { it.matchStatus == EventMatchStatus.CONFIRMED }?.globalId
        val sole = globalId != null && eventSourceRefPort.countByGlobalId(globalId) == 1L
        return if (sole) globalId else null
    }

    private data class ResolvedMatch(
        val globalId: String?,
        val status: EventMatchStatus,
        val score: Int,
    )
}
