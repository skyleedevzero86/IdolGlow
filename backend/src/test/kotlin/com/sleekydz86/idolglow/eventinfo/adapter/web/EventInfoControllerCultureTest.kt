package com.sleekydz86.idolglow.eventinfo.adapter.web

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import com.sleekydz86.idolglow.eventinfo.infrastructure.CalendarDayEvents
import com.sleekydz86.idolglow.eventinfo.infrastructure.CultureInfoApiClient
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

@DisplayName("통합 행사 목록의 문화정보 캘린더 누락 알림")
class EventInfoControllerCultureTest {
    private val cultureInfoApiClient: CultureInfoApiClient = mockk()

    private val controller =
        EventInfoController(
            festivalEventQueryUseCase = mockk { every { listByDate(any(), any(), any(), any()) } returns emptyList() },
            seoulSjwPerformApiClient = mockk { every { list(any(), any()) } returns emptyList() },
            kopisPerformanceApiClient =
                mockk { every { listPerformances(any(), any(), any(), any(), any(), any()) } returns emptyList() },
            spcdeInfoApiClient = mockk(),
            cultureInfoApiClient = cultureInfoApiClient,
            eventLocalizedTextService = mockk(),
        )

    @DisplayName("문화정보 호출이 일부 실패하면 받은 목록은 그대로 주고 경고를 남긴다")
    @Test
    fun warnsWhenCultureCalendarIsIncomplete() {
        // given
        every { cultureInfoApiClient.listEventsForCalendarDayWithCompleteness("20261120", 1, 20) } returns
            CalendarDayEvents(listOf(문화행사()), complete = false)

        // when
        val (result, logs) = 로그와_함께 { controller.festivals(date = "20261120", pageNo = 1, numOfRows = 20) }

        // then
        assertEquals(listOf("culture-1"), result.map { it.contentId })
        assertEquals(
            listOf("문화정보 캘린더 일부 호출이 실패해 화면 목록에서 빠졌습니다. date=20261120 returned=1"),
            logs,
        )
    }

    @DisplayName("문화정보 호출이 모두 성공하면 경고를 남기지 않는다")
    @Test
    fun staysQuietWhenCultureCalendarIsComplete() {
        // given
        every { cultureInfoApiClient.listEventsForCalendarDayWithCompleteness("20261120", 1, 20) } returns
            CalendarDayEvents(listOf(문화행사()), complete = true)

        // when
        val (result, logs) = 로그와_함께 { controller.festivals(date = "20261120", pageNo = 1, numOfRows = 20) }

        // then
        assertEquals(1, result.size)
        assertTrue(logs.isEmpty())
    }

    private fun <T> 로그와_함께(block: () -> T): Pair<T, List<String>> {
        val appender = ListAppender<ILoggingEvent>()
        val logger = LoggerFactory.getLogger(EventInfoController::class.java) as Logger
        appender.start()
        logger.addAppender(appender)
        try {
            return block() to appender.list.map { it.formattedMessage }
        } finally {
            logger.detachAppender(appender)
        }
    }

    private fun 문화행사(): FestivalEvent =
        FestivalEvent(
            contentId = "culture-1",
            title = "세종 체임버 시리즈",
            address = "서울 종로구 세종문화회관",
            eventStartDate = "20261120",
            eventEndDate = "20261120",
            thumbnailImageUrl = null,
            imageUrl = null,
            mapX = null,
            mapY = null,
            phone = null,
        )
}
