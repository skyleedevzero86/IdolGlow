package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMatchReasonPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceComparison
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import org.springframework.stereotype.Component

@Component
class EventMatchConflictAdvisor(
    private val eventSourceRefPort: EventSourceRefPort,
    private val reasonPort: EventMatchReasonPort,
) {
    fun suggestions(): List<EventMatchSuggestion> {
        val open = eventSourceRefPort.findByMatchStatusIn(OPEN)
        return open.filter { it.globalId.isNullOrBlank() }.mapNotNull { suggestion(it, open) }
    }

    private fun suggestion(
        ref: EventSourceRef,
        open: List<EventSourceRef>,
    ): EventMatchSuggestion? {
        val anchor = anchorFor(ref, open) ?: return null
        val facts = EventSourceComparison.of(EventSourceComparison.side(ref), EventSourceComparison.side(anchor))
        return EventMatchSuggestion(
            target = "${ref.source}|${ref.sourceId}",
            globalId = requireNotNull(anchor.globalId),
            reason = reasonPort.reason(facts),
            sameShow = facts.sameShow(),
        )
    }

    private fun anchorFor(
        ref: EventSourceRef,
        open: List<EventSourceRef>,
    ): EventSourceRef? {
        val peers =
            open.filter { peer ->
                peer.globalId?.isNotBlank() == true && !sameRef(ref, peer) && titleAndPeriod(ref, peer)
            }
        return peers.firstOrNull { venueMatches(ref, it) } ?: peers.firstOrNull()
    }

    private fun titleAndPeriod(
        left: EventSourceRef,
        right: EventSourceRef,
    ): Boolean {
        val facts = EventSourceComparison.of(EventSourceComparison.side(left), EventSourceComparison.side(right))
        return facts.sameNormalizedTitle && facts.periodsOverlap
    }

    private fun venueMatches(
        left: EventSourceRef,
        right: EventSourceRef,
    ): Boolean = EventSourceComparison.of(EventSourceComparison.side(left), EventSourceComparison.side(right)).sameVenue

    private fun sameRef(
        left: EventSourceRef,
        right: EventSourceRef,
    ): Boolean = left.source == right.source && left.sourceId == right.sourceId

    companion object {
        private val OPEN = listOf(EventMatchStatus.CANDIDATE, EventMatchStatus.CONFLICT)
    }
}
