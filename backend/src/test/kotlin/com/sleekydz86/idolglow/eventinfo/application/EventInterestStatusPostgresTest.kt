package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventSourceRefJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

@DisplayName("공연 상세의 관심 등록 상태 (Postgres)")
class EventInterestStatusPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var eventInterestService: EventInterestService

    @Autowired private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired private lateinit var eventSourceRefJpaRepository: EventSourceRefJpaRepository

    @Autowired private lateinit var userJpaRepository: UserJpaRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun clean() {
        jdbcTemplate.update("delete from event_interest")
        jdbcTemplate.update("delete from event_source_ref where source_id like 'STATUS-%'")
    }

    @DisplayName("대표 공연에 확정 연결되지 않은 원본은 globalId 없이 관심 등록 안 함으로 돌려준다")
    @Test
    fun unlinkedSourceHasNoGlobalId() {
        // given
        val user = 사용자()
        val candidate = 원본(공연(), EventMatchStatus.CANDIDATE)

        // when
        val unknown = eventInterestService.status(user.id, "KOPIS_PBLPRFR", "STATUS-none")
        val notConfirmed = eventInterestService.status(user.id, candidate.source, candidate.sourceId)

        // then
        assertEquals(EventInterestStatus(globalId = null, interested = false), unknown)
        assertEquals(EventInterestStatus(globalId = null, interested = false), notConfirmed)
    }

    @DisplayName("확정 연결된 원본은 대표 공연 ID와 관심 등록 여부를 돌려준다")
    @Test
    fun confirmedSourceReturnsGlobalIdAndInterest() {
        // given
        val user = 사용자()
        val globalId = 공연()
        val ref = 원본(globalId, EventMatchStatus.CONFIRMED)

        // when
        val before = eventInterestService.status(user.id, ref.source, ref.sourceId)
        eventInterestService.register(user.id, globalId)
        val after = eventInterestService.status(user.id, ref.source, ref.sourceId)

        // then
        assertEquals(EventInterestStatus(globalId = globalId, interested = false), before)
        assertEquals(EventInterestStatus(globalId = globalId, interested = true), after)
    }

    private fun 원본(
        globalId: String,
        status: EventMatchStatus,
    ): EventSourceRef =
        eventSourceRefJpaRepository.save(
            EventSourceRef.create("KOPIS_PBLPRFR", "STATUS-" + UUID.randomUUID().toString().take(8)).apply {
                assignMatch(globalId, status, MATCH_SCORE, LocalDateTime.now())
            },
        )

    private fun 사용자(): User = userJpaRepository.save(User.of(email = "status${UUID.randomUUID().toString().take(8)}@idolglow.test"))

    private fun 공연(): String {
        val globalId = UUID.randomUUID().toString()
        eventMasterJpaRepository.save(
            EventMaster(
                globalId = globalId,
                title = "상태 공연",
                normalizedTitle = "상태공연${globalId.take(8)}",
                startDate = LocalDate.of(2026, 12, 1),
                endDate = LocalDate.of(2026, 12, 3),
                venue = "세종문화회관",
                normalizedVenue = "세종문화회관",
                status = EventMasterStatus.ACTIVE,
            ),
        )
        return globalId
    }

    companion object {
        private const val MATCH_SCORE: Int = 80
    }
}
