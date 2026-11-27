package com.sleekydz86.idolglow.eventinfo.application

import ch.qos.logback.classic.Level
import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTitleTranslatorPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventTranslationPort
import com.sleekydz86.idolglow.eventinfo.domain.EventTitleTarget
import io.mockk.every
import io.mockk.justRun
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.springframework.dao.DataIntegrityViolationException
import java.time.LocalDate

@DisplayName("AI 공연 제목 번역 채우기")
class EventMachineTitleServiceTest {
    private val from = LocalDate.of(2026, 11, 24)
    private val to = from.plusDays(30)
    private val tourSource = TourEventTranslationCollector.TOUR_SOURCE
    private val target = EventTitleTarget("g-1", "2026 서울빛초롱축제")

    private val translationPort = mockk<EventTranslationPort>()
    private val translator =
        mockk<EventTitleTranslatorPort> {
            every { available() } returns true
            every { glossary(any()) } returns emptyList()
        }
    private val txService =
        mockk<EventTranslationTxService> {
            every { upsertMachine(any(), any(), any()) } returns true
            justRun { recordMachineFailure(any(), any(), any()) }
        }

    private fun service(maxPerLocale: Int = 100) =
        EventMachineTitleService(
            eventTranslationPort = translationPort,
            eventTitleTranslatorPort = translator,
            eventTranslationTxService = txService,
            properties = EventMachineTitleProperties(maxPerLocale = maxPerLocale),
        )

    private fun onlyJapaneseTargets(targets: List<EventTitleTarget>) {
        every { translationPort.findUntranslatedFestivals(tourSource, any(), from, to, any()) } returns emptyList()
        every { translationPort.findUntranslatedFestivals(tourSource, "ja", from, to, any()) } returns targets
    }

    @DisplayName("검사를 통과한 응답만 한국어 원래 제목을 붙여 AI 번역으로 저장한다")
    @Test
    fun savesOnlyCheckedTitle() {
        // given
        onlyJapaneseTargets(listOf(target))
        every { translator.translate("ja", target.koreanTitle, any()) } returns "2026 ソウル灯籠祭り"

        // when
        val created = service().fill(from, to)

        // then
        assertEquals(mapOf("en" to 0, "ja" to 1, "zh" to 0), created)
        verify(exactly = 1) { txService.upsertMachine("g-1", "ja", "2026 ソウル灯籠祭り（2026 서울빛초롱축제）") }
    }

    @DisplayName("검사에 걸린 응답은 저장하지 않는다")
    @Test
    fun skipsTitleThatFailsCheck() {
        // given
        onlyJapaneseTargets(listOf(target))
        every { translator.translate("ja", target.koreanTitle, any()) } returns "2025 ソウル灯籠祭り"

        // when
        val created = service().fill(from, to)

        // then
        assertEquals(0, created["ja"])
        verify(exactly = 0) { txService.upsertMachine(any(), any(), any()) }
        verify(exactly = 1) { txService.recordMachineFailure("g-1", "ja", "NUMBER_CHANGED") }
    }

    @DisplayName("AI 호출이 실패해 응답이 없는 공연은 건너뛰고 다음 공연을 계속 번역한다")
    @Test
    fun continuesAfterEngineFailure() {
        // given
        val next = EventTitleTarget("g-2", "부산 불꽃축제")
        onlyJapaneseTargets(listOf(target, next))
        every { translator.translate("ja", target.koreanTitle, any()) } returns null
        every { translator.translate("ja", next.koreanTitle, any()) } returns "釜山花火大会"

        // when
        val created = service().fill(from, to)

        // then
        assertEquals(1, created["ja"])
        verify(exactly = 0) { txService.upsertMachine("g-1", any(), any()) }
        verify(exactly = 1) { txService.recordMachineFailure("g-1", "ja", "NO_RESPONSE") }
        verify(exactly = 1) { txService.upsertMachine("g-2", "ja", "釜山花火大会（부산 불꽃축제）") }
        verify(exactly = 0) { txService.recordMachineFailure("g-2", any(), any()) }
    }

    @DisplayName("한 공연의 저장이 제약 위반으로 실패해도 다음 공연을 계속 저장한다")
    @Test
    fun continuesAfterSaveConflict() {
        // given
        val next = EventTitleTarget("g-2", "부산 불꽃축제")
        onlyJapaneseTargets(listOf(target, next))
        every { translator.translate("ja", target.koreanTitle, any()) } returns "2026 ソウル灯籠祭り"
        every { translator.translate("ja", next.koreanTitle, any()) } returns "釜山花火大会"
        every { txService.upsertMachine("g-1", "ja", any()) } throws
            DataIntegrityViolationException("uk_event_translation_global_locale")

        // when
        val created = service().fill(from, to)

        // then
        assertEquals(1, created["ja"])
        verify(exactly = 1) { txService.upsertMachine("g-2", "ja", "釜山花火大会（부산 불꽃축제）") }
    }

    @DisplayName("빈 응답은 저장하지 않는다")
    @Test
    fun skipsBlankResponse() {
        // given
        onlyJapaneseTargets(listOf(target))
        every { translator.translate("ja", target.koreanTitle, any()) } returns " "

        // when
        val created = service().fill(from, to)

        // then
        assertEquals(0, created["ja"])
        verify(exactly = 0) { txService.upsertMachine(any(), any(), any()) }
        verify(exactly = 1) { txService.recordMachineFailure("g-1", "ja", "NO_RESPONSE") }
    }

    @DisplayName("번역 모델을 쓸 수 없으면 대상을 고르지도 않는다")
    @Test
    fun skipsWhenEngineUnavailable() {
        // given
        every { translator.available() } returns false

        // when
        val created = service().fill(from, to)

        // then
        assertEquals(emptyMap<String, Int>(), created)
        verify(exactly = 0) { translationPort.findUntranslatedFestivals(any(), any(), any(), any(), any()) }
    }

    @DisplayName("언어마다 설정한 최대 건수만 고른다")
    @Test
    fun asksForConfiguredLimitPerLocale() {
        // given
        every { translationPort.findUntranslatedFestivals(tourSource, any(), from, to, 7) } returns emptyList()

        // when
        service(maxPerLocale = 7).fill(from, to)

        // then
        listOf("en", "ja", "zh").forEach { locale ->
            verify(exactly = 1) { translationPort.findUntranslatedFestivals(tourSource, locale, from, to, 7) }
        }
    }

    @DisplayName("고른 대상 수가 언어당 상한과 같으면 그 언어에 경고 로그를 남긴다")
    @Test
    fun warnsWhenTargetsReachLimit() {
        // given
        val next = EventTitleTarget("g-2", "부산 불꽃축제")
        onlyJapaneseTargets(listOf(target, next))
        every { translator.translate(any(), any(), any()) } returns null

        // when
        val events = captureLogs { service(maxPerLocale = 2).fill(from, to) }

        // then
        val warnings = events.filter { it.level == Level.WARN && it.formattedMessage.contains("상한에 닿았습니다") }
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().formattedMessage.contains("locale=ja limit=2"))
    }

    @DisplayName("고른 대상 수가 상한보다 적으면 경고 로그를 남기지 않는다")
    @Test
    fun doesNotWarnBelowLimit() {
        // given
        onlyJapaneseTargets(listOf(target))
        every { translator.translate(any(), any(), any()) } returns null

        // when
        val events = captureLogs { service(maxPerLocale = 2).fill(from, to) }

        // then
        assertTrue(events.none { it.level == Level.WARN })
    }

    private fun captureLogs(action: () -> Unit): List<ILoggingEvent> {
        val appender = ListAppender<ILoggingEvent>()
        val logger = LoggerFactory.getLogger(EventMachineTitleService::class.java) as Logger
        appender.start()
        logger.addAppender(appender)
        try {
            action()
            return appender.list.toList()
        } finally {
            logger.detachAppender(appender)
        }
    }
}
