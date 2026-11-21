package com.sleekydz86.idolglow.eventinfo.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("관광 다국어 축제의 좌표·기간 연결 판정")
class FestivalCoordinateMatchTest {
    @DisplayName("기간이 같고 좌표가 200m 안인 국문 축제가 1건이면 그 축제를 고른다")
    @Test
    fun picksSingleKoreanFestivalWithSamePeriodNearby() {
        // given
        val english = 축제("1037621", 129.3775942753, 36.0530051077)
        val korean = listOf(축제("588175", 129.3776, 36.0531), 축제("3395188", 127.1, 35.4))

        // when
        val match = FestivalCoordinateMatch.findSingle(english, korean)

        // then
        assertEquals("588175", match?.contentId)
    }

    @DisplayName("좌표가 같아도 기간이 다르면 고르지 않는다")
    @Test
    fun skipsWhenPeriodDiffers() {
        // given
        val english = 축제("4135897", 126.9767679222, 37.5716249337, end = "20270103")
        val korean = listOf(축제("3035607", 126.9767679222, 37.5716249337, end = "20270110"))

        // when
        val match = FestivalCoordinateMatch.findSingle(english, korean)

        // then
        assertNull(match)
    }

    @DisplayName("기간과 좌표가 맞는 국문 축제가 2건이면 애매해서 고르지 않는다")
    @Test
    fun skipsWhenTwoKoreanFestivalsMatch() {
        // given
        val english = 축제("E1", 126.9767, 37.5716)
        val korean = listOf(축제("K1", 126.9767, 37.5716), 축제("K2", 126.9768, 37.5717))

        // when
        val match = FestivalCoordinateMatch.findSingle(english, korean)

        // then
        assertNull(match)
    }

    @DisplayName("좌표가 200m보다 멀면 고르지 않는다")
    @Test
    fun skipsWhenFartherThanLimit() {
        // given
        val english = 축제("E1", 126.9767, 37.5716)
        val korean = listOf(축제("K1", 126.9767, 37.5746))

        // when
        val match = FestivalCoordinateMatch.findSingle(english, korean)

        // then
        assertNull(match)
    }

    @DisplayName("좌표가 없거나 0이면 고르지 않는다")
    @Test
    fun skipsWhenCoordinateIsMissing() {
        // given
        val korean = listOf(축제("K1", 0.0, 0.0), 축제("K2", 126.9767, 37.5716))

        // when
        val missing = FestivalCoordinateMatch.findSingle(축제("E1", null, null), korean)
        val zero = FestivalCoordinateMatch.findSingle(축제("E2", 0.0, 0.0), korean)

        // then
        assertNull(missing)
        assertNull(zero)
    }

    private fun 축제(
        contentId: String,
        mapX: Double?,
        mapY: Double?,
        start: String = "20261119",
        end: String = "20261121",
    ): FestivalEvent =
        FestivalEvent(
            contentId = contentId,
            title = contentId,
            address = null,
            eventStartDate = start,
            eventEndDate = end,
            thumbnailImageUrl = null,
            imageUrl = null,
            mapX = mapX,
            mapY = mapY,
            phone = null,
        )
}
