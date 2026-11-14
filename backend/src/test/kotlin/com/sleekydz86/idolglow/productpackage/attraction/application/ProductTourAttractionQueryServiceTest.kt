package com.sleekydz86.idolglow.productpackage.attraction.application

import com.sleekydz86.idolglow.productpackage.attraction.application.port.out.TourAttractionQueryPort
import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("상품 주변 관광지 추천")
class ProductTourAttractionQueryServiceTest {
    private val port = mockk<TourAttractionQueryPort>()
    private val service = ProductTourAttractionQueryService(port)

    private val attraction =
        TourAttraction("A1", "하늘공원", 1, "서울특별시", 11440, "마포구", "관광지", "자연관광지", 1, null, null, "202610")

    @DisplayName("요청한 달에 자료가 없으면 지난달 자료로 대체하고 요청한 달을 함께 돌려준다")
    @Test
    fun fallsBackToPreviousMonthWhenRequestedMonthIsEmpty() {
        // given
        every { port.fetchAreaBasedAttractions("202611", 11, 11440, any()) } returns emptyList()
        every { port.fetchAreaBasedAttractions("202610", 11, 11440, any()) } returns listOf(attraction)

        // when
        val result = service.findAttractionsByProduct(1L, 8, "202611", null, 11, 11440)

        // then
        assertEquals("202610", result.baseYm)
        assertEquals("202611", result.requestedBaseYm)
        assertEquals(listOf("하늘공원"), result.attractions.map { it.name })
        assertTrue(result.attractions.single().reason.contains("202610"))
    }

    @DisplayName("요청한 달에 자료가 있으면 그 달을 그대로 쓴다")
    @Test
    fun keepsRequestedMonthWhenItHasData() {
        // given
        every { port.fetchAreaBasedAttractions("202610", 11, 11440, any()) } returns listOf(attraction)

        // when
        val result = service.findAttractionsByProduct(1L, 8, "202610", null, 11, 11440)

        // then
        assertEquals("202610", result.baseYm)
        assertEquals("202610", result.requestedBaseYm)
        verify(exactly = 0) { port.fetchAreaBasedAttractions("202609", any(), any(), any()) }
    }

    @DisplayName("두 달 전까지 모두 비어 있으면 요청한 달 기준 빈 목록을 돌려준다")
    @Test
    fun returnsEmptyAfterTwoMonthsBack() {
        // given
        every { port.fetchAreaBasedAttractions(any(), 11, 11440, any()) } returns emptyList()

        // when
        val result = service.findAttractionsByProduct(1L, 8, "202611", null, 11, 11440)

        // then
        assertEquals("202611", result.baseYm)
        assertTrue(result.attractions.isEmpty())
        verify(exactly = 3) { port.fetchAreaBasedAttractions(any(), 11, 11440, any()) }
    }
}
