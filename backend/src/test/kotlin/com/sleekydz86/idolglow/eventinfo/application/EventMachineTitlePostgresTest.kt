package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMaster
import com.sleekydz86.idolglow.eventinfo.domain.EventMasterStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.domain.EventTitleTarget
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationAttempt
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventMasterJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventOutboxJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventSourceRefJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventTranslationAttemptJpaRepository
import com.sleekydz86.idolglow.eventinfo.infrastructure.EventTranslationJpaRepository
import com.sleekydz86.idolglow.support.PostgresIntegrationTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.LocalDate
import java.time.LocalDateTime

@DisplayName("AI 공연 제목 대상 고르기와 출처 대체 (Postgres)")
class EventMachineTitlePostgresTest : PostgresIntegrationTest() {
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

    @DisplayName("기간이 겹치고 관광 축제로 확정 연결되며 그 언어 번역이 없는 공연만 시작일 순으로 고른다")
    @Test
    fun picksConfirmedFestivalsWithoutTranslationInWindow() {
        // given
        festival("g-late", "늦은 축제", from.plusDays(20), from.plusDays(22))
        festival("g-early", "이른 축제", from.minusDays(3), from.plusDays(1))
        festival("g-done", "번역된 축제", from.plusDays(2), from.plusDays(3))
        festival("g-other-locale", "영문만 있는 축제", from.plusDays(4), from.plusDays(5))
        festival("g-candidate", "후보 축제", from.plusDays(2), from.plusDays(3), EventMatchStatus.CANDIDATE)
        festival("g-past", "끝난 축제", from.minusDays(10), from.minusDays(1))
        festival("g-future", "먼 축제", to.plusDays(1), to.plusDays(2))
        master("g-no-ref", "연결 없는 공연", from.plusDays(1), from.plusDays(2))
        eventTranslationTxService.upsertProvider("g-done", "ja", "翻訳済み", null)
        eventTranslationTxService.upsertProvider("g-other-locale", "en", "English only", null)

        // when
        val targets = eventTranslationPort.findUntranslatedFestivals(tourSource, "ja", from, to, 10)

        // then
        assertEquals(
            listOf(
                EventTitleTarget("g-early", "이른 축제"),
                EventTitleTarget("g-other-locale", "영문만 있는 축제"),
                EventTitleTarget("g-late", "늦은 축제"),
            ),
            targets,
        )
    }

    @DisplayName("병합된 대표 공연은 빼고, 종료일이 없으면 시작일로 기간을 판단한다")
    @Test
    fun skipsMergedAndUsesStartDateWhenEndDateMissing() {
        // given
        festival("g-merged", "병합된 축제", from.plusDays(1), from.plusDays(2), masterStatus = EventMasterStatus.MERGED)
        festival("g-open", "종료일 없는 축제", from.plusDays(3), null)
        festival("g-open-past", "종료일 없는 지난 축제", from.minusDays(1), null)

        // when
        val targets = eventTranslationPort.findUntranslatedFestivals(tourSource, "en", from, to, 10)

        // then
        assertEquals(listOf("g-open"), targets.map { it.globalId })
    }

    @DisplayName("언어마다 최대 건수까지만 고른다")
    @Test
    fun limitsTargets() {
        // given
        festival("g-1", "첫 축제", from.plusDays(1), from.plusDays(2))
        festival("g-2", "둘째 축제", from.plusDays(3), from.plusDays(4))

        // when
        val targets = eventTranslationPort.findUntranslatedFestivals(tourSource, "zh", from, to, 1)

        // then
        assertEquals(listOf("g-1"), targets.map { it.globalId })
    }

    @DisplayName("공식 번역은 AI 번역을 대체하고 변경 기록을 남기며, AI 번역은 공식 번역을 덮지 않는다")
    @Test
    fun providerReplacesMachineButMachineNeverReplacesProvider() {
        // given
        festival("g-1", "서울빛초롱축제", from.plusDays(1), from.plusDays(2))
        eventTranslationTxService.upsertMachine("g-1", "ja", "ソウル灯籠祭り（서울빛초롱축제）")
        val outboxAfterMachine = eventOutboxJpaRepository.count()

        // when
        eventTranslationTxService.upsertProvider("g-1", "ja", "ソウル光初籠祭り", null)
        val overwritten = eventTranslationTxService.upsertMachine("g-1", "ja", "別の翻訳（서울빛초롱축제）")

        // then
        val stored = eventTranslationJpaRepository.findByGlobalIdAndLocale("g-1", "ja")!!
        assertEquals(EventTranslationOrigin.PROVIDER, stored.origin)
        assertEquals("ソウル光初籠祭り", stored.title)
        assertFalse(overwritten)
        assertEquals(1L, outboxAfterMachine)
        assertEquals(2L, eventOutboxJpaRepository.count())
        assertTrue(eventOutboxJpaRepository.findAll().all { it.eventType == "TRANSLATION_CHANGED" })
    }

    @DisplayName("실패 기록이 없는 공연이 먼저 오고, 실패한 공연끼리는 그 언어의 마지막 실패가 오래된 순서로 온다")
    @Test
    fun putsFailedFestivalsAfterUntriedOnesByOldestFailure() {
        // given
        festival("g-untried", "처음 시도할 축제", from.plusDays(10), from.plusDays(11))
        festival("g-failed-early", "일찍 시작하는 실패 축제", from.plusDays(1), from.plusDays(2))
        festival("g-failed-recent", "최근 실패 축제", from.plusDays(3), from.plusDays(4))
        festival("g-failed-old", "오래전 실패 축제", from.plusDays(5), from.plusDays(6))
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

    @DisplayName("같은 공연·언어의 AI 번역 실패를 다시 기록하면 행은 하나로 두고 횟수를 늘리며 마지막 이유를 바꾼다")
    @Test
    fun recordsRepeatedFailureInOneRow() {
        // given
        festival("g-1", "서울빛초롱축제", from.plusDays(1), from.plusDays(2))
        eventTranslationTxService.recordMachineFailure("g-1", "ja", "NUMBER_CHANGED,HANGUL_LEFT")
        val first = attemptJpaRepository.findByGlobalIdAndLocale("g-1", "ja")!!

        // when
        eventTranslationTxService.recordMachineFailure("g-1", "ja", "NO_RESPONSE")

        // then
        val stored = attemptJpaRepository.findByGlobalIdAndLocale("g-1", "ja")!!
        assertEquals(1L, attemptJpaRepository.count())
        assertEquals(1, first.failCount)
        assertEquals(2, stored.failCount)
        assertEquals("NO_RESPONSE", stored.lastReason)
        assertFalse(stored.lastFailedAt.isBefore(first.lastFailedAt))
    }

    @DisplayName("실패 이유는 문자 수로 200자까지 그대로 저장하고 201자면 앞 200자만 저장한다")
    @Test
    fun storesReasonUpTo200Characters() {
        // given
        festival("g-200", "이백 자 축제", from.plusDays(1), from.plusDays(2))
        festival("g-201", "이백일 자 축제", from.plusDays(1), from.plusDays(2))
        val exact = "가".repeat(100) + "灯".repeat(99) + "😀"
        val over = "😀".repeat(201)

        // when
        eventTranslationTxService.recordMachineFailure("g-200", "zh", exact)
        eventTranslationTxService.recordMachineFailure("g-201", "zh", over)

        // then
        assertEquals(exact, attemptJpaRepository.findByGlobalIdAndLocale("g-200", "zh")!!.lastReason)
        assertEquals("😀".repeat(200), attemptJpaRepository.findByGlobalIdAndLocale("g-201", "zh")!!.lastReason)
    }

    @DisplayName("번역 행이 AI·공식·사람 어느 출처로 저장되든 그 공연·언어의 실패 기록만 지운다")
    @Test
    fun savingTranslationClearsOnlyThatFailure() {
        // given
        festival("g-machine", "AI 축제", from.plusDays(1), from.plusDays(2))
        festival("g-provider", "공식 축제", from.plusDays(1), from.plusDays(2))
        festival("g-manual", "사람 축제", from.plusDays(1), from.plusDays(2))
        listOf("g-machine", "g-provider", "g-manual").forEach {
            eventTranslationTxService.recordMachineFailure(it, "ja", "NO_RESPONSE")
        }
        eventTranslationTxService.recordMachineFailure("g-machine", "en", "NO_RESPONSE")

        // when
        eventTranslationTxService.upsertMachine("g-machine", "ja", "AI祭り（AI 축제）")
        eventTranslationTxService.upsertProvider("g-provider", "ja", "公式祭り", null)
        eventTranslationTxService.saveManual("g-manual", "ja", "手入力の祭り")

        // then
        assertEquals(
            listOf("g-machine" to "en"),
            attemptJpaRepository.findAll().map { it.globalId to it.locale },
        )
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
        end: LocalDate?,
        status: EventMatchStatus = EventMatchStatus.CONFIRMED,
        masterStatus: EventMasterStatus = EventMasterStatus.ACTIVE,
    ) {
        master(globalId, title, start, end, masterStatus)
        eventSourceRefJpaRepository.saveAndFlush(
            EventSourceRef(
                source = TourEventTranslationCollector.TOUR_SOURCE,
                sourceId = "tour-$globalId",
                globalId = globalId,
                matchStatus = status,
                matchScore = 100,
                rawHash = "hash",
                title = title,
                startDate = start,
                endDate = end,
            ),
        )
    }

    private fun master(
        globalId: String,
        title: String,
        start: LocalDate,
        end: LocalDate?,
        masterStatus: EventMasterStatus = EventMasterStatus.ACTIVE,
    ): EventMaster =
        eventMasterJpaRepository.saveAndFlush(
            EventMaster(
                globalId = globalId,
                title = title,
                normalizedTitle = title,
                startDate = start,
                endDate = end,
                venue = null,
                normalizedVenue = null,
                status = masterStatus,
            ),
        )
}
