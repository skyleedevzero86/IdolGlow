package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@DisplayName("문화정보 공연 수집 어댑터")
class CultureInfoPerformanceBatchAdapterTest {
    private val client = mockk<CultureInfoApiClient>()
    private val adapter = CultureInfoPerformanceBatchAdapter(client)
    private val from = LocalDate.of(2026, 11, 19)
    private val to = LocalDate.of(2026, 11, 21)

    @DisplayName("모든 날짜를 받으면 누락 사유가 없다")
    @Test
    fun hasNoReasonWhenAllDaysComplete() {

        // given
        하루_결과("20261119", complete = true, "1")
        하루_결과("20261120", complete = true, "2")
        하루_결과("20261121", complete = true, "3")

        // when
        val batch = adapter.fetchBatch(from, to)

        // then
        assertEquals(3, batch.items.size)
        assertNull(batch.incompleteReason)
    }

    @DisplayName("일부 날짜가 빠지면 받은 공연은 두고 누락 날짜 수를 사유로 남긴다")
    @Test
    fun keepsItemsAndReportsIncompleteDays() {

        // given
        하루_결과("20261119", complete = true, "1")
        하루_결과("20261120", complete = false)
        하루_결과("20261121", complete = true, "3")

        // when
        val batch = adapter.fetchBatch(from, to)

        // then
        assertEquals(2, batch.items.size)
        assertTrue(batch.incompleteReason.orEmpty().contains("3일 중 1일"))
    }

    @DisplayName("모든 날짜가 빠지면 예외로 알린다")
    @Test
    fun throwsWhenEveryDayIncomplete() {

        // given
        하루_결과("20261119", complete = false)
        하루_결과("20261120", complete = false)
        하루_결과("20261121", complete = false)

        // when
        val error = assertThrows<IllegalStateException> { adapter.fetchBatch(from, to) }

        // then
        assertTrue(error.message.orEmpty().contains("모든 날짜에서 실패"))
    }

    private fun 하루_결과(
        day: String,
        complete: Boolean,
        vararg contentIds: String,
    ) {
        every { client.listEventsForCalendarDayWithCompleteness(day, 1, any()) } returns
            CalendarDayEvents(contentIds.map { 공연(it, day) }, complete)
    }

    private fun 공연(
        contentId: String,
        day: String,
    ): FestivalEvent =
        FestivalEvent(
            contentId = contentId,
            title = "공연 $contentId",
            address = null,
            eventStartDate = day,
            eventEndDate = day,
            thumbnailImageUrl = null,
            imageUrl = null,
            mapX = null,
            mapY = null,
            phone = null,
            detailUrl = null,
            category = null,
            synopsis = null,
            source = "CULTURE_CALENDAR_A",
        )
}
