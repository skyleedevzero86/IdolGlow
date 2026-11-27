package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaApplyResult
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaSnapshot
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMetaProjectionJpaRepository
import com.sleekydz86.idolglow.notification.infrastructure.NotificationJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.util.UUID
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@DisplayName("공연 메타 이벤트 소비자 동시 처리 (Postgres)")
class EventMetaConsumerConcurrencyPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var eventMetaProjectionService: EventMetaProjectionService

    @Autowired private lateinit var eventInterestAlertService: EventInterestAlertService

    @Autowired private lateinit var eventInterestService: EventInterestService

    @Autowired private lateinit var eventMetaProjectionJpaRepository: EventMetaProjectionJpaRepository

    @Autowired private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired private lateinit var notificationJpaRepository: NotificationJpaRepository

    @Autowired private lateinit var userJpaRepository: UserJpaRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    @AfterEach
    fun clean() {
        jdbcTemplate.update("delete from consumed_event")
        jdbcTemplate.update("delete from event_meta_projection")
        jdbcTemplate.update("delete from event_interest")
        jdbcTemplate.update("delete from notifications where type = 'EVENT_INTEREST_CHANGED'")
    }

    @DisplayName("T8 읽기 모델 소비자 8개가 같은 이벤트를 동시에 처리해도 한 번만 반영되고, 다시 전달되면 중복으로 건너뛴다")
    @Test
    fun projectionAppliesConcurrentDeliveryOnce() {
        // given
        val message = eventMessage("gid-t8", title = "동시 처리 제목")

        // when
        val outcomes = runConcurrently(CONSUMERS) { eventMetaProjectionService.apply(message) }
        val redelivered = eventMetaProjectionService.apply(message)

        // then
        assertOnlyOneApplied(outcomes)
        assertEquals(EventMetaApplyResult.DUPLICATE, redelivered)
        val projection = eventMetaProjectionJpaRepository.findById("gid-t8").get()
        assertEquals(1L to "동시 처리 제목", projection.metaVersion to projection.title)
        assertEquals(listOf("APPLIED"), recordedResults(EventMetaProjectionService.CONSUMER, message.eventId))
    }

    @DisplayName("T8 관심 공연 알림 소비자 8개가 같은 일정 변경 이벤트를 동시에 처리해도 알림은 한 번만 간다")
    @Test
    fun interestAlertSendsOnceUnderConcurrentDelivery() {
        // given
        val user = userJpaRepository.save(User.of(email = "race${UUID.randomUUID().toString().take(8)}@idolglow.test"))
        val globalId = persistEvent()
        eventInterestService.register(user.id, globalId)
        val message = eventMessage(globalId, title = "관심 공연", changed = listOf("startDate"))

        // when
        val outcomes = runConcurrently(CONSUMERS) { eventInterestAlertService.handle(message) }
        val redelivered = eventInterestAlertService.handle(message)

        // then
        assertOnlyOneApplied(outcomes)
        assertEquals(EventMetaApplyResult.DUPLICATE, redelivered)
        assertEquals(1, notificationJpaRepository.findAllByUserIdOrderByCreatedAtDesc(user.id).size)
        assertEquals(listOf("APPLIED"), recordedResults(EventInterestAlertService.CONSUMER, message.eventId))
    }

    private fun assertOnlyOneApplied(outcomes: List<Result<EventMetaApplyResult>>) {
        val results = outcomes.mapNotNull { it.getOrNull() }
        val failures = outcomes.mapNotNull { it.exceptionOrNull() }
        assertEquals(1, results.count { it == EventMetaApplyResult.APPLIED })
        assertTrue(results.all { it == EventMetaApplyResult.APPLIED || it == EventMetaApplyResult.DUPLICATE }) {
            "반영도 중복도 아닌 결과가 있습니다: $results"
        }
        assertTrue(failures.all { it is DataIntegrityViolationException }) {
            "유일 제약 위반이 아닌 예외가 있습니다: ${failures.map { it::class.simpleName }}"
        }
    }

    private fun runConcurrently(
        count: Int,
        action: () -> EventMetaApplyResult,
    ): List<Result<EventMetaApplyResult>> {
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(count)
        val futures =
            List(count) {
                pool.submit<Result<EventMetaApplyResult>> {
                    start.await()
                    runCatching(action)
                }
            }
        start.countDown()
        val outcomes = futures.map { it.get(TIMEOUT_SECONDS, TimeUnit.SECONDS) }
        pool.shutdownNow()
        return outcomes
    }

    private fun eventMessage(
        globalId: String,
        title: String,
        changed: List<String> = listOf("title"),
    ): EventMetaMessage =
        EventMetaMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = "UPDATED",
            globalId = globalId,
            metaVersion = 1,
            occurredAt = "2026-11-23T09:00:00+09:00",
            changedFields = changed,
            snapshot =
                EventMetaSnapshot(
                    title = title,
                    startDate = LocalDate.of(2026, 12, 1),
                    endDate = LocalDate.of(2026, 12, 3),
                    venue = "세종문화회관",
                    status = "ACTIVE",
                ),
        )

    private fun recordedResults(
        consumer: String,
        eventId: String,
    ): List<String?> =
        jdbcTemplate.queryForList(
            "select result from consumed_event where consumer_name = ? and event_id = ?",
            String::class.java,
            consumer,
            eventId,
        )

    private fun persistEvent(): String {
        val globalId = UUID.randomUUID().toString()
        eventMasterJpaRepository.save(
            EventMaster(
                globalId = globalId,
                title = "관심 공연",
                normalizedTitle = "관심공연${globalId.take(8)}",
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
        private const val CONSUMERS: Int = 8
        private const val TIMEOUT_SECONDS: Long = 30
    }
}
