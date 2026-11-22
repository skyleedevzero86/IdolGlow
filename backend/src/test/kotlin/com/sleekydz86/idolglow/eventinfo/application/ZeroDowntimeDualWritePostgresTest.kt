package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.domain.EventCollectLog
import com.sleekydz86.idolglow.eventinfo.domain.EventCollectStatus
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventCollectLogJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime

@DisplayName("무중단 전환 2단계: 수집 시각을 옛 칸과 새 칸에 함께 쓰기")
class ZeroDowntimeDualWritePostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var eventCollectLogJpaRepository: EventCollectLogJpaRepository

    @Autowired private lateinit var eventMetaTxService: EventMetaTxService

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @AfterEach
    fun clean() {
        jdbcTemplate.update("delete from event_collect_log where source like 'DUAL_WRITE%'")
    }

    @DisplayName("새 서버가 저장한 기록은 옛 칸과 새 칸이 같은 순간을 가리킨다")
    @Test
    fun newServerWritesBothColumns() {
        // given
        val log =
            EventCollectLog.record(
                source = "DUAL_WRITE_NEW",
                status = EventCollectStatus.SUCCESS,
                itemCount = 3,
                errorMessage = null,
                collectedAt = LocalDateTime.of(2026, 11, 21, 21, 35, 0),
            )

        // when
        val saved = eventCollectLogJpaRepository.save(log)

        // then
        val row =
            jdbcTemplate.queryForMap(
                "select to_char(collected_at, 'YYYY-MM-DD HH24:MI') as local_time, " +
                    "to_char(collected_at_tz at time zone 'UTC', 'YYYY-MM-DD HH24:MI') as utc_time, " +
                    "(collected_at_tz at time zone 'Asia/Seoul') = collected_at as same_moment " +
                    "from event_collect_log where id = ?",
                saved.id,
            )
        assertEquals("2026-11-21 21:35", row["local_time"])
        assertEquals("2026-11-21 12:35", row["utc_time"])
        assertEquals(true, row["same_moment"])
    }

    @DisplayName("옛 서버가 새 칸 없이 쓴 기록도 새 서버가 읽을 수 있다")
    @Test
    fun newServerReadsRowsWrittenByOldServer() {
        // given
        jdbcTemplate.update(
            "insert into event_collect_log (source, status, item_count, collected_at, created_at, updated_at) " +
                "values ('DUAL_WRITE_OLD', 'SUCCESS', 1, timestamp '2026-11-20 04:00:00', now(), now())",
        )
        val id =
            jdbcTemplate.queryForObject(
                "select id from event_collect_log where source = 'DUAL_WRITE_OLD'",
                Long::class.java,
            )!!

        // when
        val loaded = eventCollectLogJpaRepository.findById(id).orElseThrow()

        // then
        assertEquals(LocalDateTime.of(2026, 11, 20, 4, 0, 0), loaded.collectedAt)
        assertNull(loaded.collectedAtTz)
    }

    @DisplayName("실제 수집 흐름으로 저장한 기록도 두 칸이 모두 채워진다")
    @Test
    fun collectFlowFillsBothColumns() {
        // given
        val source = "DUAL_WRITE_FLOW"

        // when
        eventMetaTxService.recordRun(source, EventCollectStatus.SUCCESS, 5, null)

        // then
        val sameMoment =
            jdbcTemplate.queryForObject(
                "select (collected_at_tz at time zone 'Asia/Seoul') = collected_at " +
                    "from event_collect_log where source = ?",
                Boolean::class.java,
                source,
            )
        assertTrue(sameMoment == true)
    }
}
