package com.sleekydz86.idolglow.eventinfo.application

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventSourceRefPort
import com.sleekydz86.idolglow.eventinfo.application.port.out.TourLocaleCallFailedException
import com.sleekydz86.idolglow.eventinfo.application.port.out.TourLocaleFestivalPort
import com.sleekydz86.idolglow.eventinfo.domain.EventMatchStatus
import com.sleekydz86.idolglow.eventinfo.domain.EventSourceRef
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import java.time.LocalDate

@DisplayName("관광 다국어 수집기의 국문 공연 연결")
class TourEventTranslationCollectorTest {
    private val refs = mutableMapOf<String, EventSourceRef>()
    private val refPort =
        mockk<EventSourceRefPort> {
            every { findBySourceAndSourceId("TOUR_API", any()) } answers { refs[secondArg()] }
        }
    private val txService =
        mockk<EventTranslationTxService> {
            every { upsertProvider(any(), any(), any(), any()) } just runs
        }
    private val from = LocalDate.of(2026, 11, 20)
    private val to = LocalDate.of(2026, 12, 20)

    @DisplayName("contentId가 국문 참조와 같으면 국문 목록 없이 그대로 연결한다")
    @Test
    fun linksByContentIdWithoutKoreanList() {
        // given
        refs["99"] = 참조("99", "g-1", EventMatchStatus.CONFIRMED)
        val port = 공급처(en = listOf(축제("99", "Lantern Festival")))

        // when
        수집기(port).collect(from, to)

        // then
        verify(exactly = 1) { txService.upsertProvider("g-1", "en", "Lantern Festival", null) }
        verify(exactly = 0) { port.searchFestival("ko", any(), any(), any(), any()) }
    }

    @DisplayName("contentId가 달라도 기간·좌표가 맞는 국문 축제가 1건이면 그 공연에 연결한다")
    @Test
    fun linksByCoordinateWhenContentIdDiffers() {
        // given
        refs["588175"] = 참조("588175", "g-pohang", EventMatchStatus.CONFIRMED)
        val port =
            공급처(
                en = listOf(축제("1037621", "Pohang Fireworks Festival", 129.3775942753, 36.0530051077)),
                ko = listOf(축제("588175", "포항국제불빛축제", 129.3776, 36.0531)),
            )

        // when
        수집기(port).collect(from, to)

        // then
        verify(exactly = 1) { txService.upsertProvider("g-pohang", "en", "Pohang Fireworks Festival", null) }
    }

    @DisplayName("좌표로 찾은 국문 공연이 아직 후보면 연결하지 않는다")
    @Test
    fun skipsWhenCoordinateMatchIsCandidate() {
        // given
        refs["588175"] = 참조("588175", "g-pohang", EventMatchStatus.CANDIDATE)
        val port =
            공급처(
                en = listOf(축제("1037621", "Pohang Fireworks Festival", 129.3775942753, 36.0530051077)),
                ko = listOf(축제("588175", "포항국제불빛축제", 129.3776, 36.0531)),
            )

        // when
        수집기(port).collect(from, to)

        // then
        verify(exactly = 0) { txService.upsertProvider(any(), any(), any(), any()) }
    }

    @DisplayName("국문 목록 조회가 실패해도 contentId 연결은 하고 수집을 멈추지 않는다")
    @Test
    fun keepsContentIdLinksWhenKoreanListFails() {
        // given
        refs["99"] = 참조("99", "g-1", EventMatchStatus.CONFIRMED)
        val port =
            공급처(
                en = listOf(축제("99", "Lantern Festival"), 축제("1037621", "Other", 129.37, 36.05)),
                ja = listOf(축제("99", "灯籠祭り")),
            )
        every { port.searchFestival("ko", any(), any(), any(), any()) } throws IllegalStateException("국문 호출 실패")

        // when
        수집기(port).collect(from, to)

        // then
        verify(exactly = 1) { txService.upsertProvider("g-1", "en", "Lantern Festival", null) }
        verify(exactly = 1) { txService.upsertProvider("g-1", "ja", "灯籠祭り", null) }
    }

    @DisplayName("언어별 API 호출 실패(403 등)는 원인 한 줄만 남기고 스택 트레이스를 찍지 않는다")
    @Test
    fun expectedCallFailureLogsWithoutStackTrace() {
        // given
        val port = 공급처()
        every { port.searchFestival("ja", any(), any(), any(), any()) } throws
            TourLocaleCallFailedException("관광 다국어 API 호출이 실패했습니다. locale=ja pageNo=1")

        // when
        val logs = 로그와_함께 { 수집기(port).collect(from, to) }

        // then
        val failure = logs.single { it.formattedMessage.startsWith("관광 다국어 수집에 실패했습니다. locale=ja") }
        assertEquals(
            "관광 다국어 수집에 실패했습니다. locale=ja reason=관광 다국어 API 호출이 실패했습니다. locale=ja pageNo=1",
            failure.formattedMessage,
        )
        assertNull(failure.throwableProxy)
    }

    @DisplayName("예상하지 못한 예외는 원인을 찾을 수 있게 스택 트레이스를 남긴다")
    @Test
    fun unexpectedFailureKeepsStackTrace() {
        // given
        val port = 공급처()
        every { port.searchFestival("ja", any(), any(), any(), any()) } throws IllegalArgumentException("파싱 버그")

        // when
        val logs = 로그와_함께 { 수집기(port).collect(from, to) }

        // then
        val failure = logs.single { it.formattedMessage.startsWith("관광 다국어 수집에 실패했습니다. locale=ja") }
        assertNotNull(failure.throwableProxy)
    }

    private fun 로그와_함께(block: () -> Unit): List<ILoggingEvent> {
        val appender = ListAppender<ILoggingEvent>()
        val logger = LoggerFactory.getLogger(TourEventTranslationCollector::class.java) as Logger
        appender.start()
        logger.addAppender(appender)
        try {
            block()
            return appender.list.toList()
        } finally {
            logger.detachAppender(appender)
        }
    }

    private fun 수집기(port: TourLocaleFestivalPort) = TourEventTranslationCollector(port, refPort, txService)

    private fun 공급처(
        en: List<FestivalEvent> = emptyList(),
        ja: List<FestivalEvent> = emptyList(),
        ko: List<FestivalEvent> = emptyList(),
    ): TourLocaleFestivalPort {
        val byLocale = mapOf("en" to en, "ja" to ja, "zh" to emptyList(), "ko" to ko)
        return mockk {
            every { searchFestival(any(), any(), any(), any(), any()) } answers {
                if (arg<Int>(3) == 1) byLocale[firstArg()].orEmpty() else emptyList()
            }
        }
    }

    private fun 참조(
        sourceId: String,
        globalId: String,
        status: EventMatchStatus,
    ): EventSourceRef =
        EventSourceRef(
            source = "TOUR_API",
            sourceId = sourceId,
            globalId = globalId,
            matchStatus = status,
            matchScore = 0,
            rawHash = "hash",
            title = sourceId,
        )

    private fun 축제(
        contentId: String,
        title: String,
        mapX: Double? = null,
        mapY: Double? = null,
    ): FestivalEvent =
        FestivalEvent(
            contentId = contentId,
            title = title,
            address = null,
            eventStartDate = "20261119",
            eventEndDate = "20261121",
            thumbnailImageUrl = null,
            imageUrl = null,
            mapX = mapX,
            mapY = mapY,
            phone = null,
        )
}
