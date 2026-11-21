package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaApplyResult
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaMessage
import com.sleekydz86.idolglow.eventinfo.domain.EventMetaSnapshot
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMetaProjectionJpaRepository
import com.sleekydz86.idolglow.notification.domain.NotificationType
import com.sleekydz86.idolglow.notification.infrastructure.NotificationJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.infrastructure.UserJpaRepository
import io.micrometer.core.instrument.MeterRegistry
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDate
import java.util.UUID

@DisplayName("공연 메타 이벤트 소비자 (Postgres)")
class EventMetaConsumerPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var eventMetaProjectionService: EventMetaProjectionService

    @Autowired private lateinit var eventInterestAlertService: EventInterestAlertService

    @Autowired private lateinit var eventInterestService: EventInterestService

    @Autowired private lateinit var eventMetaProjectionJpaRepository: EventMetaProjectionJpaRepository

    @Autowired private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired private lateinit var notificationJpaRepository: NotificationJpaRepository

    @Autowired private lateinit var userJpaRepository: UserJpaRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var meterRegistry: MeterRegistry

    @BeforeEach
    @AfterEach
    fun clean() {
        jdbcTemplate.update("delete from consumed_event")
        jdbcTemplate.update("delete from event_meta_projection")
        jdbcTemplate.update("delete from event_interest")
        jdbcTemplate.update("delete from notifications where type = 'EVENT_INTEREST_CHANGED'")
    }

    @DisplayName("T2 같은 이벤트를 두 번 받아도 한 번만 반영한다")
    @Test
    fun appliesSameEventOnce() {
        // given
        val message = 메시지("gid-t2", version = 1, title = "첫 제목")

        // when
        val first = eventMetaProjectionService.apply(message)
        val second = eventMetaProjectionService.apply(message.copy(snapshot = 스냅샷("다른 제목")))

        // then
        assertEquals(EventMetaApplyResult.APPLIED, first)
        assertEquals(EventMetaApplyResult.DUPLICATE, second)
        assertEquals("첫 제목", eventMetaProjectionJpaRepository.findById("gid-t2").get().title)
        assertEquals(1, 처리_건수(EventMetaProjectionService.CONSUMER))
    }

    @DisplayName("T3 v2가 먼저, v1이 나중에 오면 v2를 유지하고 버린 건수를 센다")
    @Test
    fun dropsOlderVersion() {
        // given
        val droppedBefore = 버린_건수()
        eventMetaProjectionService.apply(메시지("gid-t3", version = 2, title = "새 제목"))

        val older = 메시지("gid-t3", version = 1, title = "옛 제목")

        // when
        val result = eventMetaProjectionService.apply(older)

        // then
        assertEquals(EventMetaApplyResult.STALE, result)
        val projection = eventMetaProjectionJpaRepository.findById("gid-t3").get()
        assertEquals(2L to "새 제목", projection.metaVersion to projection.title)
        assertEquals(1.0, 버린_건수() - droppedBefore)
        assertEquals("STALE", 기록된_결과(EventMetaProjectionService.CONSUMER, older.eventId))
    }

    @DisplayName("관심 등록한 공연의 일정이 바뀌면 알림을 한 번 보내고, 번역만 바뀌면 보내지 않는다")
    @Test
    fun alertsInterestedUserOnScheduleChange() {
        // given
        val user = 사용자()
        val globalId = 공연()
        eventInterestService.register(user.id, globalId)
        val scheduleChanged = 메시지(globalId, version = 2, title = "관심 공연", changed = listOf("startDate"))
        val translationChanged = 메시지(globalId, version = 3, title = "관심 공연", changed = listOf("translation"))

        // when
        val first = eventInterestAlertService.handle(scheduleChanged)
        val duplicate = eventInterestAlertService.handle(scheduleChanged)
        val ignored = eventInterestAlertService.handle(translationChanged)

        // then
        assertEquals(
            listOf(EventMetaApplyResult.APPLIED, EventMetaApplyResult.DUPLICATE, EventMetaApplyResult.IGNORED),
            listOf(first, duplicate, ignored),
        )
        val alerts = notificationJpaRepository.findAllByUserIdOrderByCreatedAtDesc(user.id)
        assertEquals(listOf(NotificationType.EVENT_INTEREST_CHANGED), alerts.map { it.type })
        assertEquals("관심 공연 의 일정이 바뀌었습니다.", alerts.single().message)
        val consumer = EventInterestAlertService.CONSUMER
        assertEquals("APPLIED", 기록된_결과(consumer, scheduleChanged.eventId))
        assertEquals("IGNORED", 기록된_결과(consumer, translationChanged.eventId))
    }

    private fun 메시지(
        globalId: String,
        version: Long,
        title: String,
        changed: List<String> = listOf("title"),
    ): EventMetaMessage =
        EventMetaMessage(
            eventId = UUID.randomUUID().toString(),
            eventType = "UPDATED",
            globalId = globalId,
            metaVersion = version,
            occurredAt = "2026-11-21T09:00:00+09:00",
            changedFields = changed,
            snapshot = 스냅샷(title),
        )

    private fun 스냅샷(title: String): EventMetaSnapshot =
        EventMetaSnapshot(
            title = title,
            startDate = LocalDate.of(2026, 12, 1),
            endDate = LocalDate.of(2026, 12, 3),
            venue = "세종문화회관",
            status = "ACTIVE",
        )

    private fun 처리_건수(consumer: String): Int =
        jdbcTemplate.queryForObject(
            "select count(*) from consumed_event where consumer_name = ?",
            Int::class.java,
            consumer,
        )!!

    private fun 기록된_결과(
        consumer: String,
        eventId: String,
    ): String? =
        jdbcTemplate.queryForObject(
            "select result from consumed_event where consumer_name = ? and event_id = ?",
            String::class.java,
            consumer,
            eventId,
        )

    private fun 버린_건수(): Double = meterRegistry.find("event_meta_projection_stale_dropped").counter()?.count() ?: 0.0

    private fun 사용자(): User {
        val suffix = UUID.randomUUID().toString().take(8)
        return userJpaRepository.save(User.of(email = "interest$suffix@idolglow.test"))
    }

    private fun 공연(): String {
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
}
