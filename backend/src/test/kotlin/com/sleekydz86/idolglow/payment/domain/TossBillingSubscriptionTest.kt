package com.sleekydz86.idolglow.payment.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("토스 빌링 구독")
class TossBillingSubscriptionTest {
    @DisplayName("결제 성공 시 결제일은 기존 nextBillingAt 기준 한 달 뒤로 유지된다")
    @Test
    fun successKeepsNextBillingFromCurrentDue() {
        // given
        val due = LocalDateTime.of(2026, 10, 1, 0, 0)
        val paidAt = due.plusDays(3)
        val subscription = subscription(nextBillingAt = due)

        // when
        subscription.markCharged(paidAt)

        // then
        assertEquals(due.plusMonths(1), subscription.nextBillingAt)
        assertEquals(0, subscription.failCount)
        assertNull(subscription.retryAt)
        assertEquals(TossBillingSubscriptionStatus.ACTIVE, subscription.status)
    }

    @DisplayName("결제 실패 3회면 PAUSED 된다")
    @Test
    fun pausesAfterThreeFailures() {
        // given
        val now = LocalDateTime.of(2026, 10, 16, 12, 0)
        val subscription = subscription(nextBillingAt = now)

        // when
        subscription.markChargeFailed(now)
        // then
        assertEquals(1, subscription.failCount)
        assertEquals(now.plusDays(1), subscription.retryAt)

        subscription.markChargeFailed(now.plusDays(1))
        assertEquals(2, subscription.failCount)
        assertEquals(now.plusDays(1).plusDays(2), subscription.retryAt)

        subscription.markChargeFailed(now.plusDays(3))
        assertEquals(3, subscription.failCount)
        assertEquals(TossBillingSubscriptionStatus.PAUSED, subscription.status)
        assertNull(subscription.retryAt)
    }

    @DisplayName("해지 예약 기간 안에는 멤버십이 유효하고 기간이 지나면 무효다")
    @Test
    fun cancelScheduledStaysValidUntilEndsAt() {
        // given
        val endsAt = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscription = subscription(nextBillingAt = endsAt)
        // when
        subscription.scheduleCancel(LocalDateTime.of(2026, 10, 16, 0, 0))

        // then
        assertEquals(TossBillingSubscriptionStatus.CANCEL_SCHEDULED, subscription.status)
        assertTrue(subscription.isMembershipActive(endsAt.minusDays(1)))
        assertTrue(subscription.isMembershipActive(endsAt))
        assertFalse(subscription.isMembershipActive(endsAt.plusSeconds(1)))
        assertEquals(TossBillingSubscriptionStatus.CANCELED, subscription.status)
    }

    @DisplayName("3개월 주기 구독은 결제 성공 시 다음 결제일이 세 달 뒤로 간다")
    @Test
    fun quarterlyMovesNextBillingThreeMonths() {
        // given
        val due = LocalDateTime.of(2026, 11, 3, 9, 0)
        val subscription = subscription(nextBillingAt = due, cycleMonths = 3)

        // when
        subscription.markCharged(due)

        // then
        assertEquals(LocalDateTime.of(2027, 2, 3, 9, 0), subscription.nextBillingAt)
    }

    @DisplayName("1년 주기 구독은 결제 성공 시 다음 결제일이 열두 달 뒤로 간다")
    @Test
    fun yearlyMovesNextBillingTwelveMonths() {
        // given
        val due = LocalDateTime.of(2026, 11, 3, 9, 0)
        val subscription = subscription(nextBillingAt = due, cycleMonths = 12)

        // when
        subscription.markCharged(due.plusHours(2))

        // then
        assertEquals(LocalDateTime.of(2027, 11, 3, 9, 0), subscription.nextBillingAt)
    }

    @DisplayName("해지 예약하면 종료 시각은 이미 결제한 기간이 끝나는 다음 결제일이다")
    @Test
    fun cancelOfHalfYearEndsAtPaidPeriodEnd() {
        // given
        val due = LocalDateTime.of(2026, 11, 3, 9, 0)
        val subscription = subscription(nextBillingAt = due, cycleMonths = 6)
        subscription.markCharged(due)

        // when
        subscription.scheduleCancel(due.plusDays(10))

        // then
        assertEquals(LocalDateTime.of(2027, 5, 3, 9, 0), subscription.endsAt)
    }

    private fun subscription(
        nextBillingAt: LocalDateTime,
        cycleMonths: Int = 1,
    ): TossBillingSubscription {
        val method =
            TossBillingMethod(
                userId = 1L,
                customerKey = "idolglow-user-1",
                billingKey = "billing-key",
            )
        return TossBillingSubscription(
            userId = 1L,
            billingMethod = method,
            orderName = "IdolGlow 월간 구독",
            amount = 4_900L,
            nextBillingAt = nextBillingAt,
            cycleMonths = cycleMonths,
        )
    }
}
