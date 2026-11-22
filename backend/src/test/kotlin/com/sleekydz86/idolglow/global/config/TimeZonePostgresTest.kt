package com.sleekydz86.idolglow.global.config

import com.sleekydz86.idolglow.eventinfo.domain.EventCollectLog
import com.sleekydz86.idolglow.eventinfo.domain.EventCollectStatus
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventCollectLogJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import jakarta.persistence.EntityManagerFactory
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.TimeZone

@DisplayName("시간대 Asia/Seoul 고정")
class TimeZonePostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var entityManagerFactory: EntityManagerFactory

    @Autowired private lateinit var eventCollectLogJpaRepository: EventCollectLogJpaRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun clean() {
        jdbcTemplate.update("delete from event_collect_log where source = ?", SOURCE)
    }

    @DisplayName("JVM 기본 시간대는 Asia/Seoul이다")
    @Test
    fun jvmDefaultTimeZoneIsSeoul() {
        // given
        val expected = SEOUL

        // when
        val actual = TimeZone.getDefault().id

        // then
        assertEquals(expected, actual)
    }

    @DisplayName("Hibernate DB 시간대 설정은 Asia/Seoul이다")
    @Test
    fun hibernateJdbcTimeZoneIsSeoul() {
        // given
        val key = "hibernate.jdbc.time_zone"

        // when
        val actual =
            when (val value = entityManagerFactory.properties[key]) {
                is TimeZone -> value.id
                is ZoneId -> value.id
                else -> value?.toString()
            }

        // then
        assertEquals(SEOUL, actual)
    }

    @DisplayName("수집 시각은 넣은 시각 그대로 DB에 남고 9시간 밀리지 않는다")
    @Test
    fun collectedAtIsStoredAsGiven() {
        // given
        val collectedAt = LocalDateTime.of(2026, 11, 21, 21, 35, 0)

        // when
        val saved =
            eventCollectLogJpaRepository.save(
                EventCollectLog.record(
                    source = SOURCE,
                    status = EventCollectStatus.SUCCESS,
                    itemCount = 1,
                    errorMessage = null,
                    collectedAt = collectedAt,
                ),
            )

        // then
        val stored =
            jdbcTemplate.queryForObject(
                "select to_char(collected_at, 'YYYY-MM-DD HH24:MI:SS') from event_collect_log where id = ?",
                String::class.java,
                saved.id,
            )
        assertEquals("2026-11-21 21:35:00", stored)
    }

    companion object {
        private const val SEOUL: String = "Asia/Seoul"
        private const val SOURCE: String = "TIMEZONE_TEST"
    }
}
