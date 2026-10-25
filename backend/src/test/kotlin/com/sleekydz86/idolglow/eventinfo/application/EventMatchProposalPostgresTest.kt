package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.adapter.mcp.EventMatchMcpTools
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.domain.CollectedPerformance
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventPerformanceMatcher
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.ops.domain.OpsChangeStatus
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import java.time.LocalDate
import java.util.UUID

class EventMatchProposalPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var eventMatchReviewService: EventMatchReviewService

    @Autowired
    private lateinit var eventMatchConflictAdvisor: EventMatchConflictAdvisor

    @Autowired
    private lateinit var eventMatchMcpTools: EventMatchMcpTools

    @Autowired
    private lateinit var eventMasterPort: EventMasterPort

    @Autowired
    private lateinit var eventSourceRefPort: EventSourceRefPort

    @Autowired
    private lateinit var userJpaRepository: UserJpaRepository

    @Autowired
    private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun clearSecurity() {
        SecurityContextHolder.clearContext()
    }

    @Test
    fun userCannotListConflicts() {
        authenticate(user(UserRole.USER))

        val error =
            assertThrows(IllegalArgumentException::class.java) {
                eventMatchMcpTools.listMatchConflicts()
            }

        assertTrue(error.message?.contains("공연 매칭 조회 권한이 없습니다") == true)
    }

    @Test
    fun agentProposalsStayRequestedUntilAPersonApplies() {
        val token = UUID.randomUUID().toString().take(8)
        val same = "sin-$token"
        val wrong = "nut-$token"
        val apart = "ware-$token"
        anchor(same, "신해경 $token", "예술의전당")
        anchor(wrong, "호두까기 $token", "예술의전당")
        anchor(apart, "백자한상 $token", "국립중앙박물관", NEXT_START, NEXT_END)
        conflict(same, "신해경 $token [서울]", "예술의전당")
        conflict(wrong, "호두까기 $token", "국립극장")
        conflict(apart, "백자한상 $token", "국립중앙박물관")

        authenticate(user(UserRole.ADMIN))
        val listed = eventMatchMcpTools.listMatchConflicts()
        val compared = eventMatchMcpTools.compareSourceRecords("TOUR_API|conflict-$same", "TOUR_API|anchor-$same")
        val suggestions = eventMatchConflictAdvisor.suggestions().filter { it.target.contains(token) }
        val score = eventMatchReviewService.submit(suggestions)
        val first = suggestions.first()
        val proposed = eventMatchMcpTools.proposeMerge(first.target, first.globalId, first.reason)

        assertTrue(listed.any { it.contentId == "conflict-$same" && it.venue == "예술의전당" })
        assertFalse(listed.any { it.title.contains(SECRET) || it.contentId.contains(SECRET) })
        assertTrue(compared.sameVenue)
        assertFalse(compared.reason.contains(SECRET))
        assertEquals(2, score.total)
        assertEquals(1, score.correct)
        assertEquals(1, score.wrong)
        assertEquals("AI 제안 2건 중 1건 맞음, 틀린 1건은 승인 단계에서 걸러짐", score.summary())
        assertEquals(OpsChangeStatus.REQUESTED.name, proposed.status)
        assertUnapplied("conflict-$same")
        assertUnapplied("conflict-$wrong")
        assertRequest("TOUR_API|conflict-$wrong")
    }

    private fun anchor(
        id: String,
        title: String,
        venue: String,
        start: LocalDate = SAME_START,
        end: LocalDate = SAME_END,
    ): EventMaster {
        val master =
            eventMasterPort.save(
                EventMaster.open(
                    CollectedPerformance("TOUR_API", "anchor-$id", title, venue, start, end, SECRET),
                ),
            )
        save("anchor-$id", master.globalId, EventMatchStatus.CANDIDATE, title, venue, start, end)
        return master
    }

    private fun conflict(
        id: String,
        title: String,
        venue: String,
        start: LocalDate = SAME_START,
        end: LocalDate = SAME_END,
    ) {
        save("conflict-$id", null, EventMatchStatus.CONFLICT, title, venue, start, end)
    }

    private fun save(
        sourceId: String,
        globalId: String?,
        status: EventMatchStatus,
        title: String,
        venue: String,
        start: LocalDate,
        end: LocalDate,
    ) {
        eventSourceRefPort.save(
            EventSourceRef(
                source = "TOUR_API",
                sourceId = sourceId,
                globalId = globalId,
                matchStatus = status,
                matchScore = EventPerformanceMatcher.CANDIDATE_SCORE,
                rawHash = "hash",
                rawPayload = SECRET,
                title = title,
                startDate = start,
                endDate = end,
                venue = venue,
            ),
        )
    }

    private fun assertUnapplied(sourceId: String) {
        val ref =
            eventSourceRefPort.findBySourceAndSourceId("TOUR_API", sourceId)
                ?: throw IllegalStateException("공연 공급처 원본이 없습니다. sourceId=$sourceId")
        assertNull(ref.globalId)
        assertEquals(EventMatchStatus.CONFLICT, ref.matchStatus)
    }

    private fun assertRequest(target: String) {
        val rows =
            jdbcTemplate.query(
                """
                select status, requested_by, requested_by_kind, payload
                from ops_change_request
                where target = ?
                """.trimIndent(),
                { rs, _ ->
                    listOf(
                        rs.getString("status"),
                        rs.getString("requested_by"),
                        rs.getString("requested_by_kind"),
                        rs.getString("payload"),
                    )
                },
                target,
            )
        assertTrue(rows.isNotEmpty())
        assertTrue(rows.all { it[0] == OpsChangeStatus.REQUESTED.name })
        assertTrue(rows.all { it[1] == "event-match-agent" && it[2] == "AGENT" })
        assertTrue(rows.all { !it[3].contains(SECRET) })
    }

    private fun user(role: UserRole): User {
        val token = UUID.randomUUID().toString().replace("-", "").take(8)
        return userJpaRepository.save(User.of("match$token@test.com", "m$token", role))
    }

    private fun authenticate(actor: User) {
        SecurityContextHolder.getContext().authentication =
            UsernamePasswordAuthenticationToken(
                actor.id.toString(),
                "",
                listOf(SimpleGrantedAuthority("ROLE_${actor.role.name}")),
            )
    }

    companion object {
        private const val SECRET: String = "service-key-should-stay-hidden"
        private val SAME_START: LocalDate = LocalDate.of(2026, 11, 1)
        private val SAME_END: LocalDate = LocalDate.of(2026, 11, 3)
        private val NEXT_START: LocalDate = LocalDate.of(2027, 1, 1)
        private val NEXT_END: LocalDate = LocalDate.of(2027, 1, 2)
    }
}
