package com.sleekydz86.idolglow.subscription.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.DayOfWeek

@DisplayName("구독 발송 스케줄")
class SubscriptionDispatchScheduleTest {
    @DisplayName("상품 추천 메일은 8시부터 20시 사이 시각만 저장할 수 있다")
    @Test
    fun scheduleHourOutside8To20IsRejectedForRecommendation() {
        // given
        val rejectedHours = listOf(0, 7, 21, 23)
        val allowedHours = listOf(8, 14, 20)
        val existing = schedule(SubscriptionContentType.PRODUCT_RECOMMENDATION, 10)

        // when
        val createFailures =
            rejectedHours.map { hour ->
                runCatching { schedule(SubscriptionContentType.PRODUCT_RECOMMENDATION, hour) }.exceptionOrNull()
            }
        val created = allowedHours.map { schedule(SubscriptionContentType.PRODUCT_RECOMMENDATION, it) }
        val updateFailure =
            runCatching {
                existing.update(
                    frequencyType = SubscriptionDispatchFrequency.DAILY,
                    dayOfWeek = null,
                    dispatchHour = 21,
                    dispatchMinute = 0,
                    active = true,
                )
            }.exceptionOrNull()

        // then
        createFailures.forEach { assertEquals(IllegalArgumentException::class.java, it?.javaClass) }
        assertEquals(allowedHours, created.map { it.dispatchHour })
        assertEquals(IllegalArgumentException::class.java, updateFailure?.javaClass)
        assertEquals(10, existing.dispatchHour)
    }

    @DisplayName("소식지와 웹진은 지금처럼 0시부터 23시까지 저장할 수 있다")
    @Test
    fun otherContentTypesKeepFullDayRange() {
        // given
        val hours = listOf(0, 7, 21, 23)

        // when
        val newsletters = hours.map { schedule(SubscriptionContentType.NEWSLETTER, it) }
        val idolglows = hours.map { schedule(SubscriptionContentType.IDOLGLOW_ISSUE, it) }

        // then
        assertEquals(hours, newsletters.map { it.dispatchHour })
        assertEquals(hours, idolglows.map { it.dispatchHour })
    }

    @DisplayName("상품 추천 메일도 24시 범위를 벗어나면 기존 메시지로 거절한다")
    @Test
    fun recommendationStillRejectsInvalidHour() {
        // given
        val invalidHour = 24

        // when / then
        assertThrows(IllegalArgumentException::class.java) {
            schedule(SubscriptionContentType.PRODUCT_RECOMMENDATION, invalidHour)
        }
    }

    private fun schedule(
        contentType: SubscriptionContentType,
        hour: Int,
    ): SubscriptionDispatchSchedule =
        SubscriptionDispatchSchedule.create(
            contentType = contentType,
            frequencyType = SubscriptionDispatchFrequency.WEEKLY,
            dayOfWeek = DayOfWeek.MONDAY,
            dispatchHour = hour,
            dispatchMinute = 0,
            active = true,
        )
}
