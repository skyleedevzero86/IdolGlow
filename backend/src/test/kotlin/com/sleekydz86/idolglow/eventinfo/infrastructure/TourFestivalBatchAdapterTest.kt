package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

@DisplayName("관광공사 국문 축제 수집 어댑터")
class TourFestivalBatchAdapterTest {
    private val client = mockk<TourFestivalApiClient>()
    private val from = LocalDate.of(2026, 11, 20)
    private val to = LocalDate.of(2026, 12, 20)

    @DisplayName("국문 서비스로 받은 축제를 사유 없이 돌려준다")
    @Test
    fun returnsKoreanFestivals() {
        // given
        every { client.searchFestivalInLocale("ko", "20261120", "20261220", 1, 100) } returns listOf(축제("588175"))

        // when
        val batch = TourFestivalBatchAdapter(client).fetchBatch(from, to)

        // then
        assertEquals(listOf("588175"), batch.items.map { it.sourceId })
        assertEquals("TOUR_API", batch.items.single().source)
        assertNull(batch.incompleteReason)
    }

    @DisplayName("첫 쪽 호출이 실패하면 0건 성공 대신 예외로 알린다")
    @Test
    fun throwsWhenFirstPageFails() {
        // given
        every { client.searchFestivalInLocale("ko", any(), any(), 1, 100) } throws
            IllegalStateException("관광 다국어 API 호출이 실패했습니다. locale=ko pageNo=1")

        // when
        val error = assertThrows<IllegalStateException> { TourFestivalBatchAdapter(client).fetchBatch(from, to) }

        // then
        assertEquals("관광 다국어 API 호출이 실패했습니다. locale=ko pageNo=1", error.message)
    }

    private fun 축제(contentId: String): FestivalEvent =
        FestivalEvent(
            contentId = contentId,
            title = "포항국제불빛축제",
            address = "경상북도 포항시 북구 해안로 48",
            eventStartDate = "20261119",
            eventEndDate = "20261121",
            thumbnailImageUrl = null,
            imageUrl = null,
            mapX = 129.3776,
            mapY = 36.0531,
            phone = null,
        )
}
