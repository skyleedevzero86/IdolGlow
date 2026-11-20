package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMatchReasonPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchFacts
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceComparison
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.domain.EventTitleNormalizer
import org.springframework.stereotype.Component

@Component
class EventMatchConflictAdvisor(
    private val eventSourceRefPort: EventSourceRefPort,
    private val eventMasterPort: EventMasterPort,
    private val reasonPort: EventMatchReasonPort,
) {
    fun suggestions(): List<EventMatchSuggestion> =
        eventSourceRefPort
            .findByMatchStatusIn(OPEN)
            .filter { it.globalId.isNullOrBlank() }
            .mapNotNull(::suggestion)

    private fun suggestion(ref: EventSourceRef): EventMatchSuggestion? {
        val (anchor, facts) = anchorFor(ref) ?: return null
        return EventMatchSuggestion(
            target = "${ref.source}|${ref.sourceId}",
            globalId = anchor.globalId,
            reason = reasonPort.reason(facts),
            sameShow = facts.sameShow(),
        )
    }

    private fun anchorFor(ref: EventSourceRef): Pair<EventMaster, EventMatchFacts>? {
        val side = EventSourceComparison.side(ref)
        val peers =
            eventMasterPort
                .findActiveByNormalizedTitle(EventTitleNormalizer.normalize(ref.title))
                .map { master -> master to EventSourceComparison.of(side, EventSourceComparison.side(master)) }
                .filter { (_, facts) -> facts.sameNormalizedTitle && facts.periodsOverlap }
        return peers.firstOrNull { (_, facts) -> facts.sameVenue } ?: peers.firstOrNull()
    }

    companion object {
        private val OPEN = listOf(EventMatchStatus.CANDIDATE, EventMatchStatus.CONFLICT)
    }
}
