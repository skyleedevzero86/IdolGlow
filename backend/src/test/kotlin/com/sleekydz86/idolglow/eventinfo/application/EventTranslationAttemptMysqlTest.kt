package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationAttempt
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventOutboxJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventSourceRefJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventTranslationAttemptJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventTranslationJpaRepository
import com.sleekydz86.idolglow.support.MysqlIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("AI 공연 제목 번역 실패 기록 (MySQL)")
class EventTranslationAttemptMysqlTest : MysqlIntegrationTest() {
    @Autowired
    private lateinit var eventTranslationPort: EventTranslationPort

    @Autowired
    private lateinit var eventTranslationTxService: EventTranslationTxService

    @Autowired
    private lateinit var eventMasterJpaRepository: EventMasterJpaRepository

    @Autowired
    private lateinit var eventSourceRefJpaRepository: EventSourceRefJpaRepository

    @Autowired
    private lateinit var eventTranslationJpaRepository: EventTranslationJpaRepository

    @Autowired
    private lateinit var eventOutboxJpaRepository: EventOutboxJpaRepository

    @Autowired
    private lateinit var attemptJpaRepository: EventTranslationAttemptJpaRepository

    private val from = LocalDate.of(2026, 11, 24)
    private val to = from.plusDays(30)
    private val tourSource = TourEventTranslationCollector.TOUR_SOURCE

    @BeforeEach
    fun clean() {
        eventOutboxJpaRepository.deleteAllInBatch()
        attemptJpaRepository.deleteAllInBatch()
        eventTranslationJpaRepository.deleteAllInBatch()
        eventSourceRefJpaRepository.deleteAllInBatch()
        eventMasterJpaRepository.deleteAllInBatch()
    }

    @DisplayName("실패 기록이 없는 공연이 먼저 오고, 실패한 공연끼리는 그 언어의 마지막 실패가 오래된 순서로 온다")
    @Test
    fun putsFailedFestivalsAfterUntriedOnesByOldestFailure() {
        // given
        festival("g-untried", "처음 시도할 축제", from.plusDays(10))
        festival("g-failed-early", "일찍 시작하는 실패 축제", from.plusDays(1))
        festival("g-failed-recent", "최근 실패 축제", from.plusDays(3))
        festival("g-failed-old", "오래전 실패 축제", from.plusDays(5))
        attempt("g-failed-early", "ja", LocalDateTime.of(2026, 11, 23, 10, 0))
        attempt("g-failed-recent", "ja", LocalDateTime.of(2026, 11, 23, 12, 0))
        attempt("g-failed-old", "ja", LocalDateTime.of(2026, 11, 22, 10, 0))
        attempt("g-untried", "en", LocalDateTime.of(2026, 11, 24, 9, 0))

        // when
        val targets = eventTranslationPort.findUntranslatedFestivals(tourSource, "ja", from, to, 10)

        // then
        assertEquals(
            listOf("g-untried", "g-failed-old", "g-failed-early", "g-failed-recent"),
            targets.map { it.globalId },
        )
    }

    @DisplayName("실패 이유는 문자 수로 200자까지 그대로 저장하고 201자면 앞 200자만 저장한다")
    @Test
    fun storesReasonUpTo200Characters() {
        // given
        festival("g-200", "이백 자 축제", from.plusDays(1))
        festival("g-201", "이백일 자 축제", from.plusDays(1))
        val exact = "가".repeat(100) + "灯".repeat(99) + "😀"
        val over = "😀".repeat(201)

        // when
        eventTranslationTxService.recordMachineFailure("g-200", "zh", exact)
        eventTranslationTxService.recordMachineFailure("g-201", "zh", over)

        // then
        assertEquals(exact, attemptJpaRepository.findByGlobalIdAndLocale("g-200", "zh")!!.lastReason)
        assertEquals("😀".repeat(200), attemptJpaRepository.findByGlobalIdAndLocale("g-201", "zh")!!.lastReason)
    }

    private fun attempt(
        globalId: String,
        locale: String,
        failedAt: LocalDateTime,
    ) {
        attemptJpaRepository.saveAndFlush(EventTranslationAttempt.first(globalId, locale, "NO_RESPONSE", failedAt))
    }

    private fun festival(
        globalId: String,
        title: String,
        start: LocalDate,
    ) {
        eventMasterJpaRepository.saveAndFlush(
            EventMaster(
                globalId = globalId,
                title = title,
                normalizedTitle = title,
                startDate = start,
                endDate = start.plusDays(1),
                venue = null,
                normalizedVenue = null,
                status = EventMasterStatus.ACTIVE,
            ),
        )
        eventSourceRefJpaRepository.saveAndFlush(
            EventSourceRef(
                source = tourSource,
                sourceId = "tour-$globalId",
                globalId = globalId,
                matchStatus = EventMatchStatus.CONFIRMED,
                matchScore = 100,
                rawHash = "hash",
                title = title,
                startDate = start,
                endDate = start.plusDays(1),
            ),
        )
    }
}
