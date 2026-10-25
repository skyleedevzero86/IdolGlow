package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMatchReasonPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceComparison
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.ops.application.OpsChangeCommand
import com.sleekydz86.idolglow.ops.application.OpsChangeCommandService
import com.sleekydz86.idolglow.ops.domain.OpsActorKind
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class EventMatchReviewService(
    private val eventSourceRefPort: EventSourceRefPort,
    private val eventMasterPort: EventMasterPort,
    private val opsChangeCommandService: OpsChangeCommandService,
    private val reasonPort: EventMatchReasonPort,
    private val objectMapper: ObjectMapper,
    @Value("\${app.mcp.event-match.agent-name:event-match-agent}")
    private val agentName: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun listConflicts(): List<EventMatchConflictView> {
        requireAdmin()
        return eventSourceRefPort.findByMatchStatusIn(OPEN).map(::view)
    }

    fun compare(
        leftTarget: String,
        rightTarget: String,
    ): EventMatchComparisonView {
        requireAdmin()
        val left = load(leftTarget)
        val right = load(rightTarget)
        val facts = EventSourceComparison.of(EventSourceComparison.side(left), EventSourceComparison.side(right))
        return EventMatchComparisonView(
            left = view(left),
            right = view(right),
            sameNormalizedTitle = facts.sameNormalizedTitle,
            periodsOverlap = facts.periodsOverlap,
            sameVenue = facts.sameVenue,
            reason = reasonPort.reason(facts),
        )
    }

    fun proposeMerge(
        target: String,
        globalId: String,
        reason: String?,
    ): EventMatchProposalView {
        requireAdmin()
        val ref = load(target)
        val text = clip(reason?.trim()?.takeIf { it.isNotEmpty() } ?: reasonFor(ref, globalId))
        val saved =
            opsChangeCommandService.request(
                OpsChangeCommand(
                    type = EventMatchMergeHandler.TYPE,
                    target = target,
                    payload =
                        objectMapper.writeValueAsString(
                            EventMatchMergePayload(globalId = globalId, reason = text),
                        ),
                    kind = OpsActorKind.AGENT,
                    agentName = agentName,
                ),
            )
        return EventMatchProposalView(requestId = saved.id, status = saved.status.name, reason = text)
    }

    fun submit(suggestions: List<EventMatchSuggestion>): EventMatchProposalScore {
        requireAdmin()
        suggestions.forEach { proposeMerge(it.target, it.globalId, it.reason) }
        val correct = suggestions.count { it.sameShow }
        val score = EventMatchProposalScore(suggestions.size, correct, suggestions.size - correct)
        log.info("공연 매칭 {}", score.summary())
        return score
    }

    private fun reasonFor(
        ref: EventSourceRef,
        globalId: String,
    ): String {
        val master =
            eventMasterPort.findByGlobalId(globalId)
                ?: throw IllegalArgumentException("글로벌 공연을 찾을 수 없습니다. globalId=$globalId")
        val facts = EventSourceComparison.of(EventSourceComparison.side(ref), EventSourceComparison.side(master))
        return reasonPort.reason(facts)
    }

    private fun load(target: String): EventSourceRef {
        val parts = target.split('|', limit = 2)
        require(parts.size == 2 && parts[0].isNotBlank() && parts[1].isNotBlank()) {
            "공연 대상 형식은 공급처|공급처ID 입니다. target=$target"
        }
        return eventSourceRefPort.findBySourceAndSourceId(parts[0], parts[1])
            ?: throw IllegalArgumentException("공연 공급처 원본을 찾을 수 없습니다. target=$target")
    }

    private fun view(ref: EventSourceRef): EventMatchConflictView =
        EventMatchConflictView(
            source = ref.source,
            contentId = ref.sourceId,
            title = ref.title,
            startDate = ref.startDate?.toString(),
            endDate = ref.endDate?.toString(),
            venue = ref.venue,
            matchStatus = ref.matchStatus.name,
            globalId = ref.globalId,
        )

    private fun clip(text: String): String = text.take(clipLimit())

    private fun requireAdmin() {
        val roles =
            SecurityContextHolder
                .getContext()
                .authentication
                ?.authorities
                ?.mapNotNull { it.authority?.removePrefix("ROLE_") }
                .orEmpty()
        if (UserRole.ADMIN.name !in roles) {
            throw IllegalArgumentException("공연 매칭 조회 권한이 없습니다. role=${roles.joinToString(",")}")
        }
    }

    companion object {
        private val OPEN = listOf(EventMatchStatus.CANDIDATE, EventMatchStatus.CONFLICT)

        private fun clipLimit(): Int = (2 shl 2) * (2 shl 2) * (2 shl 1)
    }
}
