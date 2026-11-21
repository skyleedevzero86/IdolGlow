package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate

@DisplayName("무중단 전환 3단계: 옛 기록의 수집 시각을 새 칸으로 옮기기")
class ZeroDowntimeBackfillPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var backfillService: EventCollectLogTimeZoneBackfillService

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @BeforeEach
    fun fillOtherRows() {
        jdbcTemplate.update(
            "update event_collect_log set collected_at_tz = collected_at at time zone 'Asia/Seoul' " +
                "where collected_at_tz is null",
        )
    }

    @AfterEach
    fun clean() {
        jdbcTemplate.update("delete from event_collect_log where source like 'BACKFILL%'")
    }

    @DisplayName("옛 기록 20만 건을 1000건씩 옮기면 모두 채워지고 두 칸이 같은 순간을 가리킨다")
    @Test
    fun fillsLargeTableInBatches() {
        // given
        insertOldRows("BACKFILL_BULK", BULK_ROWS)

        // when
        val startedAt = System.nanoTime()
        val result = backfillService.backfill(batchSize = BATCH)
        val elapsedMillis = (System.nanoTime() - startedAt) / NANOS_PER_MILLI

        // then
        assertEquals(BULK_ROWS, result.filled)
        assertEquals(BULK_ROWS / BATCH, result.batches)
        assertEquals(0L, result.remaining)
        assertEquals(0L, mismatches("BACKFILL_BULK"))
        assertTrue(elapsedMillis < BULK_LIMIT_MILLIS, "20만 건 옮기기에 ${elapsedMillis}ms 걸렸다")
    }

    @DisplayName("중간에 멈췄다가 다시 실행하면 남은 것과 그사이 옛 서버가 쓴 기록까지 이어서 채운다")
    @Test
    fun resumesAfterStopAndCatchesNewOldRows() {
        // given
        insertOldRows("BACKFILL_RESUME", RESUME_ROWS)
        val stopped = backfillService.backfill(batchSize = SMALL_BATCH, maxBatches = 2)
        insertOldRows("BACKFILL_RESUME_LATE", LATE_ROWS)

        // when
        val resumed = backfillService.backfill(batchSize = SMALL_BATCH)

        // then
        assertEquals(SMALL_BATCH * 2, stopped.filled)
        assertEquals((RESUME_ROWS - SMALL_BATCH * 2 + LATE_ROWS).toLong(), stopped.remaining + LATE_ROWS)
        assertEquals(RESUME_ROWS - SMALL_BATCH * 2 + LATE_ROWS, resumed.filled)
        assertEquals(0L, resumed.remaining)
        assertEquals(0L, mismatches("BACKFILL_RESUME%"))
    }

    @DisplayName("새 서버가 이미 채운 값은 덮어쓰지 않는다")
    @Test
    fun keepsValuesWrittenByNewServer() {
        // given
        jdbcTemplate.update(
            "insert into event_collect_log " +
                "(source, status, item_count, collected_at, collected_at_tz, created_at, updated_at) " +
                "values ('BACKFILL_KEEP', 'SUCCESS', 1, timestamp '2026-11-21 21:35:00', " +
                "timestamptz '2000-01-01 00:00:00+00', now(), now())",
        )

        // when
        backfillService.backfill(batchSize = SMALL_BATCH)

        // then
        val kept =
            jdbcTemplate.queryForObject(
                "select to_char(collected_at_tz at time zone 'UTC', 'YYYY-MM-DD HH24:MI') " +
                    "from event_collect_log where source = 'BACKFILL_KEEP'",
                String::class.java,
            )
        assertEquals("2000-01-01 00:00", kept)
    }

    private fun insertOldRows(
        source: String,
        rows: Int,
    ) {
        jdbcTemplate.update(
            "insert into event_collect_log (source, status, item_count, collected_at, created_at, updated_at) " +
                "select ?, 'SUCCESS', n % 100, " +
                "timestamp '2026-11-01 04:00:00' + n * interval '1 minute', now(), now() " +
                "from generate_series(1, ?) as n",
            source,
            rows,
        )
    }

    private fun mismatches(sourcePattern: String): Long =
        jdbcTemplate.queryForObject(
            "select count(*) from event_collect_log where source like ? and " +
                "(collected_at_tz is null or (collected_at_tz at time zone 'Asia/Seoul') <> collected_at)",
            Long::class.java,
            sourcePattern,
        )!!

    companion object {
        private const val BULK_ROWS: Int = 200_000
        private const val BATCH: Int = 1_000
        private const val SMALL_BATCH: Int = 100
        private const val RESUME_ROWS: Int = 1_000
        private const val LATE_ROWS: Int = 50
        private const val NANOS_PER_MILLI: Long = 1_000_000
        private const val BULK_LIMIT_MILLIS: Long = 120_000
    }
}
