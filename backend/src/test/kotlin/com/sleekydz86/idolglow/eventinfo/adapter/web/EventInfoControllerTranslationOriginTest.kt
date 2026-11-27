package com.sleekydz86.idolglow.eventinfo.adapter.web

import com.sleekydz86.idolglow.eventinfo.application.EventLocalizedTextService
import com.sleekydz86.idolglow.eventinfo.domain.EventLocalizedText
import com.sleekydz86.idolglow.eventinfo.domain.EventTranslationOrigin
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import com.sleekydz86.idolglow.eventinfo.infrastructure.CalendarDayEvents
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

@DisplayName("통합 행사 목록의 번역 출처 표시")
class EventInfoControllerTranslationOriginTest {
    private val eventLocalizedTextService: EventLocalizedTextService = mockk()

    private val controller =
        EventInfoController(
            festivalEventQueryUseCase =
                mockk {
                    every { listByDate(any(), any(), any(), any()) } returns
                        listOf(축제("tour-ai", "빛초롱축제"), 축제("tour-official", "불꽃축제"), 축제("tour-ko", "국악축제"))
                },
            seoulSjwPerformApiClient = mockk { every { list(any(), any()) } returns emptyList() },
            kopisPerformanceApiClient =
                mockk { every { listPerformances(any(), any(), any(), any(), any(), any()) } returns emptyList() },
            spcdeInfoApiClient = mockk(),
            cultureInfoApiClient =
                mockk {
                    every { listEventsForCalendarDayWithCompleteness(any(), any(), any()) } returns
                        CalendarDayEvents(emptyList(), complete = true)
                },
            eventLocalizedTextService = eventLocalizedTextService,
        )

    @BeforeEach
    fun bindRequest() {
        val request = MockHttpServletRequest()
        request.addParameter("lang", "ja")
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    @AfterEach
    fun clearRequest() {
        RequestContextHolder.resetRequestAttributes()
    }

    @DisplayName("고른 번역의 출처를 담고, 한국어 원문이면 비워 둔다")
    @Test
    fun carriesOriginOfPickedTranslation() {
        // given
        every { eventLocalizedTextService.resolve(any(), "ja") } returns
            listOf(
                EventLocalizedText("光籠祭り（빛초롱축제）", null, "ja", EventTranslationOrigin.MACHINE),
                EventLocalizedText("花火大会", null, "ja", EventTranslationOrigin.PROVIDER),
                EventLocalizedText("국악축제", null, "ko", null),
            )

        // when
        val result = controller.festivals(date = "20261124", pageNo = 1, numOfRows = 20)

        // then
        assertEquals(listOf("MACHINE", "PROVIDER", null), result.map { it.translationOrigin })
        assertEquals(listOf("光籠祭り（빛초롱축제）", "花火大会", "국악축제"), result.map { it.title })
        assertEquals(listOf("ja", "ja", "ko"), result.map { it.resolvedLocale })
    }

    private fun 축제(
        contentId: String,
        title: String,
    ): FestivalEvent =
        FestivalEvent(
            contentId = contentId,
            title = title,
            address = null,
            eventStartDate = "20261124",
            eventEndDate = "20261124",
            thumbnailImageUrl = null,
            imageUrl = null,
            mapX = null,
            mapY = null,
            phone = null,
        )
}
