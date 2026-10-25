package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventCollectLogPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventMasterPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.PerformanceSourceBatchPort
import com.sleekydz86.idolglow.eventinfo.domain.CollectedPerformance
import com.sleekydz86.idolglow.eventinfo.domain.EventCollectStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventCollectLogJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventSourceRefJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate

class EventMetaMatchPostgresTest : PostgresIntegrationTest() {
    @Autowired
    private lateinit var eventMetaTxService: EventMetaTxService

    @Autowired
    private lateinit var eventMasterPort: EventMasterPort

    @Autowired
    private lateinit var eventSourceRefPort: EventSourceRefPort

    @Autowired
    private lateinit var eventCollectLogPort: EventCollectLogPort

    @Autowired
    private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired
    private lateinit var eventSourceRefJpaRepository: EventSourceRefJpaRepository

    @Autowired
    private lateinit var eventCollectLogJpaRepository: EventCollectLogJpaRepository

    @BeforeEach
    fun clean() {
        eventSourceRefJpaRepository.deleteAllInBatch()
        eventCollectLogJpaRepository.deleteAllInBatch()
        eventMasterJpaRepository.deleteAllInBatch()
    }

    @Test
    fun sameShowWithDifferentWordingSharesGlobalId() {
        val service = collectService(FixedPerformanceSource("KOPIS_PBLPRFR", overlapExamples()))

        service.collect(FROM, TO)

        val shin =
            eventSourceRefPort.findAll().filter { ref ->
                ref.sourceId == "PF304184" || ref.sourceId == "SJW-SHIN"
            }
        assertEquals(1, shin.mapNotNull { it.globalId }.toSet().size)
        assertTrue(shin.all { it.matchStatus == EventMatchStatus.CONFIRMED })
        assertEquals(5L, eventSourceRefPort.countByMatchStatus(EventMatchStatus.CONFIRMED))
        assertEquals(1L, eventSourceRefPort.countByMatchStatus(EventMatchStatus.CANDIDATE))
        assertEquals(1L, eventSourceRefPort.countByMatchStatus(EventMatchStatus.CONFLICT))
        val nutcracker =
            eventSourceRefPort.findAll().single { it.sourceId == "SJW-NUT" }
        assertEquals(EventMatchStatus.CANDIDATE, nutcracker.matchStatus)
        val spanning = eventSourceRefPort.findAll().single { it.sourceId == "CULTURE-NUT-SPAN" }
        assertEquals(EventMatchStatus.CONFLICT, spanning.matchStatus)
        assertNull(spanning.globalId)
    }

    @Test
    fun sameTitleDifferentPeriodStaysSeparate() {
        val service =
            collectService(
                FixedPerformanceSource(
                    "KOPIS_PBLPRFR",
                    listOf(
                        performance(
                            source = "KOPIS_PBLPRFR",
                            sourceId = "PF303504",
                            title = "백자한상 (10.23)",
                            venue = "국립중앙박물관",
                            start = LocalDate.of(2025, 10, 23),
                            end = LocalDate.of(2025, 10, 23),
                        ),
                        performance(
                            source = "KOPIS_PBLPRFR",
                            sourceId = "PF303506",
                            title = "백자한상 (10.24)",
                            venue = "국립중앙박물관",
                            start = LocalDate.of(2026, 10, 24),
                            end = LocalDate.of(2026, 10, 24),
                        ),
                    ),
                ),
            )

        service.collect(FROM, TO)

        val refs = eventSourceRefPort.findAll()
        assertEquals(2, eventMasterPort.count().toInt())
        assertEquals(2, refs.mapNotNull { it.globalId }.toSet().size)
        assertTrue(refs.all { it.matchStatus == EventMatchStatus.CONFIRMED })
    }

    @Test
    fun secondBatchDoesNotIncreaseGlobalIds() {
        val service =
            collectService(
                FixedPerformanceSource(
                    "KOPIS_PBLPRFR",
                    listOf(
                        performance(
                            source = "KOPIS_PBLPRFR",
                            sourceId = "PF304184",
                            title = "신해경 단독 콘서트 [서울]",
                            venue = "세종문화회관 대극장",
                            start = LocalDate.of(2026, 11, 1),
                            end = LocalDate.of(2026, 11, 2),
                        ),
                    ),
                ),
                FixedPerformanceSource(
                    "SEOUL_SJW_PERFORM",
                    listOf(
                        performance(
                            source = "SEOUL_SJW_PERFORM",
                            sourceId = "SJW-SHIN",
                            title = "신해경 단독 콘서트",
                            venue = "세종문화회관대극장",
                            start = LocalDate.of(2026, 11, 1),
                            end = LocalDate.of(2026, 11, 2),
                        ),
                    ),
                ),
            )

        service.collect(FROM, TO)
        val first = eventMasterPort.count()
        service.collect(FROM, TO)

        assertEquals(1L, first)
        assertEquals(first, eventMasterPort.count())
        assertEquals(2, eventSourceRefPort.findAll().size)
    }

    @Test
    fun failedSourceDoesNotBlockOtherSources() {
        val service =
            collectService(
                FailingPerformanceSource("SEOUL_SJW_PERFORM"),
                FixedPerformanceSource(
                    "KOPIS_PBLPRFR",
                    listOf(
                        performance(
                            source = "KOPIS_PBLPRFR",
                            sourceId = "PF304184",
                            title = "신해경 단독 콘서트 [서울]",
                            venue = "세종문화회관 대극장",
                            start = LocalDate.of(2026, 11, 1),
                            end = LocalDate.of(2026, 11, 2),
                        ),
                    ),
                ),
                FixedPerformanceSource(
                    "CULTURE_INFO",
                    listOf(
                        performance(
                            source = "CULTURE_INFO",
                            sourceId = "CULTURE-1",
                            title = "문화가 있는 날",
                            venue = "세종문화회관",
                            start = LocalDate.of(2026, 11, 3),
                            end = LocalDate.of(2026, 11, 3),
                        ),
                    ),
                ),
            )

        service.collect(FROM, TO)

        val logs = eventCollectLogPort.findAll()
        assertEquals(2, eventMasterPort.count().toInt())
        assertEquals(setOf("PF304184", "CULTURE-1"), eventSourceRefPort.findAll().map { it.sourceId }.toSet())
        assertEquals(1, logs.count { it.status == EventCollectStatus.FAILED && it.source == "SEOUL_SJW_PERFORM" })
        assertEquals(2, logs.count { it.status == EventCollectStatus.SUCCESS })
    }

    private fun collectService(vararg ports: PerformanceSourceBatchPort): EventMetaCollectService =
        EventMetaCollectService(
            sources = ports.toList(),
            eventMetaTxService = eventMetaTxService,
            eventSourceRefPort = eventSourceRefPort,
        )

    private fun overlapExamples(): List<CollectedPerformance> =
        listOf(
            performance(
                source = "KOPIS_PBLPRFR",
                sourceId = "PF304184",
                title = "신해경 단독 콘서트 [서울]",
                venue = "세종문화회관 대극장",
                start = LocalDate.of(2026, 11, 1),
                end = LocalDate.of(2026, 11, 2),
            ),
            performance(
                source = "SEOUL_SJW_PERFORM",
                sourceId = "SJW-SHIN",
                title = "신해경 단독 콘서트",
                venue = "세종문화회관대극장",
                start = LocalDate.of(2026, 11, 1),
                end = LocalDate.of(2026, 11, 2),
            ),
            performance(
                source = "KOPIS_PBLPRFR",
                sourceId = "PF-NUT-ART",
                title = "호두까기 인형",
                venue = "예술의전당",
                start = LocalDate.of(2026, 12, 1),
                end = LocalDate.of(2026, 12, 20),
            ),
            performance(
                source = "SEOUL_SJW_PERFORM",
                sourceId = "SJW-NUT",
                title = "호두까기 인형 (국립발레단)",
                venue = "국립극장",
                start = LocalDate.of(2026, 12, 5),
                end = LocalDate.of(2026, 12, 15),
            ),
            performance(
                source = "KOPIS_PBLPRFR",
                sourceId = "PF-NUT-JAN",
                title = "호두까기 인형 [서울]",
                venue = "롯데콘서트홀",
                start = LocalDate.of(2026, 1, 1),
                end = LocalDate.of(2026, 1, 10),
            ),
            performance(
                source = "KOPIS_PBLPRFR",
                sourceId = "PF-NUT-FEB",
                title = "호두까기 인형 [부산]",
                venue = "롯데콘서트홀",
                start = LocalDate.of(2026, 2, 1),
                end = LocalDate.of(2026, 2, 10),
            ),
            performance(
                source = "CULTURE_INFO",
                sourceId = "CULTURE-NUT-SPAN",
                title = "호두까기 인형",
                venue = "롯데콘서트홀",
                start = LocalDate.of(2026, 1, 5),
                end = LocalDate.of(2026, 2, 5),
            ),
        )

    private fun performance(
        source: String,
        sourceId: String,
        title: String,
        venue: String,
        start: LocalDate,
        end: LocalDate,
    ): CollectedPerformance =
        CollectedPerformance(
            source = source,
            sourceId = sourceId,
            title = title,
            venue = venue,
            startDate = start,
            endDate = end,
            rawPayload = "$source|$sourceId|$title|$venue|$start|$end",
        )

    companion object {
        private val FROM: LocalDate = LocalDate.of(2026, 1, 1)
        private val TO: LocalDate = LocalDate.of(2026, 12, 31)
    }
}

private class FixedPerformanceSource(
    override val source: String,
    private val items: List<CollectedPerformance>,
) : PerformanceSourceBatchPort {
    override fun fetch(
        from: LocalDate,
        to: LocalDate,
    ): List<CollectedPerformance> = items
}

private class FailingPerformanceSource(
    override val source: String,
) : PerformanceSourceBatchPort {
    override fun fetch(
        from: LocalDate,
        to: LocalDate,
    ): List<CollectedPerformance> {
        throw IllegalStateException("세종문화회관 인증키가 유효하지 않습니다.")
    }
}
