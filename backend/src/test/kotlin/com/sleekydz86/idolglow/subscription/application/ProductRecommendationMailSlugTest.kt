package com.sleekydz86.idolglow.subscription.application

import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchFrequency
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("상품 추천 메일 주기 키")
class ProductRecommendationMailSlugTest {
    @DisplayName("매주 발송은 ISO 주 연도와 두 자리 주차를 쓴다")
    @Test
    fun weeklySlugUsesIsoWeek() {
        // given
        val lastDayOf2026 = LocalDate.of(2026, 12, 31)
        val firstMondayOf2027 = LocalDate.of(2027, 1, 4)
        val januaryFirst2027 = LocalDate.of(2027, 1, 1)

        // when
        val slugs =
            listOf(lastDayOf2026, firstMondayOf2027, januaryFirst2027).map {
                ProductRecommendationMailSlug.of(SubscriptionDispatchFrequency.WEEKLY, it)
            }

        // then
        assertEquals(
            listOf("product-reco-2026-W53", "product-reco-2027-W01", "product-reco-2026-W53"),
            slugs,
        )
    }

    @DisplayName("매일 발송은 날짜를 그대로 쓴다")
    @Test
    fun dailySlugUsesDate() {
        // given
        val date = LocalDate.of(2026, 10, 26)

        // when
        val slug = ProductRecommendationMailSlug.of(SubscriptionDispatchFrequency.DAILY, date)

        // then
        assertEquals("product-reco-2026-10-26", slug)
        assertEquals("2026-10-26", ProductRecommendationMailSlug.periodOf(slug))
    }
}
