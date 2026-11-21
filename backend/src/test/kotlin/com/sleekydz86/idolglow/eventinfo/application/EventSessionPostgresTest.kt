package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSessionPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.PerformanceScheduleSourcePort
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSessionStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventSessionJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventSourceRefJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.transaction.support.TransactionTemplate
import java.time.LocalDate

@DisplayName("공연 → 회차 동기화")
class EventSessionPostgresTest : PostgresIntegrationTest() {
    @Autowired private lateinit var eventSessionPort: EventSessionPort

    @Autowired private lateinit var eventSessionTxService: EventSessionTxService

    @Autowired private lateinit var eventSessionQueryService: EventSessionQueryService

    @Autowired private lateinit var eventMasterMergeHandler: EventMasterMergeHandler

    @Autowired private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired private lateinit var eventSourceRefJpaRepository: EventSourceRefJpaRepository

    @Autowired private lateinit var eventSessionJpaRepository: EventSessionJpaRepository

    @Autowired private lateinit var jdbcTemplate: JdbcTemplate

    @Autowired private lateinit var transactionTemplate: TransactionTemplate

    private val source = FakeScheduleSource()

    private val syncService: EventSessionSyncService by lazy {
        EventSessionSyncService(listOf(source), eventSessionPort, eventSessionTxService)
    }

    @BeforeEach
    fun clean() {
        eventSessionJpaRepository.deleteAllInBatch()
        jdbcTemplate.update("delete from event_outbox")
        eventSourceRefJpaRepository.deleteAllInBatch()
        eventMasterJpaRepository.deleteAllInBatch()
    }

    @DisplayName("처음 동기화하면 기간 안의 회차를 만들고 변경 이벤트를 1건 남기며, 원본이 그대로면 다시 부르지 않는다")
    @Test
    fun firstSyncCreatesSessionsOnce() {
        // given
        공연("gid-a")
        원본("PF-A", "gid-a", END_OF_DEC)
        source.guidance["PF-A"] = "토요일 ~ 일요일(15:00,19:00)"

        // when
        syncService.sync(TODAY)
        syncService.sync(TODAY)

        // then
        val sessions = eventSessionQueryService.sessions("gid-a", TODAY, END_OF_DEC)
        assertEquals(16, sessions.size)
        assertEquals("2026-12-05T15:00+09:00", sessions.first().startsAt.toString())
        assertEquals(1, source.calls)
        assertEquals(1, 변경_이벤트("gid-a"))
    }

    @DisplayName("원본이 바뀌어 공연 시간이 줄면 빠진 회차는 지우지 않고 취소로 바꾼다")
    @Test
    fun changedGuidanceCancelsRemovedSessions() {
        // given
        공연("gid-a")
        원본("PF-A", "gid-a", END_OF_DEC)
        source.guidance["PF-A"] = "토요일 ~ 일요일(15:00,19:00)"
        syncService.sync(TODAY)
        원본_내용_변경("PF-A")
        source.guidance["PF-A"] = "토요일(15:00)"

        // when
        syncService.sync(TODAY)

        // then
        val byStatus =
            eventSessionQueryService.sessions("gid-a", TODAY, END_OF_DEC).groupingBy { it.status }.eachCount()
        assertEquals(mapOf(EventSessionStatus.SCHEDULED to 4, EventSessionStatus.CANCELLED to 12), byStatus)
        assertEquals(2, source.calls)
        assertEquals(2, 변경_이벤트("gid-a"))
    }

    @DisplayName("날짜가 지나 90일 범위가 늘어나면 상세를 다시 부르지 않고 새 날짜의 회차만 더한다")
    @Test
    fun rollingWindowAddsSessionsWithoutRefetch() {
        // given
        공연("gid-a")
        원본("PF-A", "gid-a", LocalDate.of(2027, 12, 31))
        source.guidance["PF-A"] = "수요일(20:00)"
        syncService.sync(TODAY)
        val before = eventSessionQueryService.sessions("gid-a", TODAY, LocalDate.of(2027, 3, 3)).size

        // when
        syncService.sync(TODAY.plusDays(7))

        // then
        val after = eventSessionQueryService.sessions("gid-a", TODAY, LocalDate.of(2027, 3, 3)).size
        assertEquals(13 to 14, before to after)
        assertEquals(1, source.calls)
        assertEquals(1, 변경_이벤트("gid-a"))
    }

    @DisplayName("상세 호출이 실패하면 아무것도 기록하지 않고 다음 동기화에서 다시 부른다")
    @Test
    fun failedFetchIsRetried() {
        // given
        공연("gid-a")
        원본("PF-A", "gid-a", END_OF_DEC)
        source.failing += "PF-A"
        syncService.sync(TODAY)
        val afterFailure = eventSessionQueryService.sessions("gid-a", TODAY, END_OF_DEC).size
        source.failing.clear()
        source.guidance["PF-A"] = "금요일(19:30)"

        // when
        syncService.sync(TODAY)

        // then
        assertEquals(0 to 4, afterFailure to eventSessionQueryService.sessions("gid-a", TODAY, END_OF_DEC).size)
        assertEquals(2, source.calls)
    }

    @DisplayName("아직 확정되지 않은 원본은 상세를 부르지 않는다")
    @Test
    fun candidateRefIsNotFetched() {
        // given
        공연("gid-a")
        원본("PF-A", "gid-a", END_OF_DEC, EventMatchStatus.CANDIDATE)
        source.guidance["PF-A"] = "금요일(19:30)"

        // when
        syncService.sync(TODAY)

        // then
        assertEquals(0, source.calls)
        assertEquals(0L, eventSessionJpaRepository.count())
    }

    @DisplayName("대표 공연을 합치면 회차가 남는 공연 아래로 보이고, 되돌리면 원래 공연으로 돌아온다")
    @Test
    fun sessionsFollowMasterMerge() {
        // given
        공연("gid-old")
        공연("gid-new")
        원본("PF-A", "gid-old", END_OF_DEC)
        source.guidance["PF-A"] = "금요일(19:30)"
        syncService.sync(TODAY)
        val payload = """{"intoGlobalId":"gid-new"}"""
        val snapshot = eventMasterMergeHandler.snapshot("gid-old")

        // when
        transactionTemplate.executeWithoutResult { eventMasterMergeHandler.apply("gid-old", payload) }
        val merged = eventSessionQueryService.sessionsOfSource("KOPIS_PBLPRFR", "PF-A", TODAY, END_OF_DEC)
        val oldAfterMerge = eventSessionQueryService.sessions("gid-old", TODAY, END_OF_DEC).size
        transactionTemplate.executeWithoutResult { eventMasterMergeHandler.revert("gid-old", snapshot) }
        val restored = eventSessionQueryService.sessions("gid-old", TODAY, END_OF_DEC).size

        // then
        assertEquals("gid-new" to 4, merged.globalId to merged.sessions.size)
        assertEquals(0 to 4, oldAfterMerge to restored)
    }

    private fun 공연(globalId: String) {
        eventMasterJpaRepository.save(
            EventMaster(
                globalId = globalId,
                title = "회차 공연 $globalId",
                normalizedTitle = "회차공연" + globalId.replace("-", ""),
                startDate = TODAY,
                endDate = END_OF_DEC,
                venue = "세종문화회관",
                normalizedVenue = "세종문화회관",
                status = EventMasterStatus.ACTIVE,
            ),
        )
    }

    private fun 원본(
        sourceId: String,
        globalId: String,
        endDate: LocalDate,
        status: EventMatchStatus = EventMatchStatus.CONFIRMED,
    ) {
        eventSourceRefJpaRepository.save(
            EventSourceRef(
                source = "KOPIS_PBLPRFR",
                sourceId = sourceId,
                globalId = globalId,
                matchStatus = status,
                matchScore = 100,
                rawHash = "hash-$sourceId",
                title = "회차 공연 $sourceId",
                startDate = TODAY,
                endDate = endDate,
            ),
        )
    }

    private fun 원본_내용_변경(sourceId: String) {
        jdbcTemplate.update(
            "update event_source_ref set raw_hash = ? where source_id = ?",
            "hash-$sourceId-v2",
            sourceId,
        )
    }

    private fun 변경_이벤트(globalId: String): Int =
        checkNotNull(
            jdbcTemplate.queryForObject(
                "select count(*) from event_outbox where aggregate_id = ? and event_type = 'SESSIONS_CHANGED'",
                Int::class.java,
                globalId,
            ),
        )

    private class FakeScheduleSource : PerformanceScheduleSourcePort {
        override val source: String = "KOPIS_PBLPRFR"
        val guidance: MutableMap<String, String> = mutableMapOf()
        val failing: MutableSet<String> = mutableSetOf()
        var calls: Int = 0

        override fun guidance(sourceId: String): String {
            calls += 1
            check(sourceId !in failing) { "KOPIS 호출 실패. path=pblprfr/$sourceId status=500" }
            return guidance[sourceId].orEmpty()
        }
    }

    companion object {
        private val TODAY: LocalDate = LocalDate.of(2026, 12, 1)
        private val END_OF_DEC: LocalDate = LocalDate.of(2026, 12, 31)
    }
}
