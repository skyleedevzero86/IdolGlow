package com.sleekydz86.idolglow.payment.domain

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TossBillingSubscriptionTest {
    @Test
    fun `결제 성공 시 결제일은 기존 nextBillingAt 기준 한 달 뒤로 유지된다`() {
        val due = LocalDateTime.of(2026, 10, 1, 0, 0)
        val paidAt = due.plusDays(3)
        val subscription = subscription(nextBillingAt = due)

        subscription.markCharged(paidAt)

        assertEquals(due.plusMonths(1), subscription.nextBillingAt)
        assertEquals(0, subscription.failCount)
        assertNull(subscription.retryAt)
        assertEquals(TossBillingSubscriptionStatus.ACTIVE, subscription.status)
    }

    @Test
    fun `결제 실패 3회면 PAUSED 된다`() {
        val now = LocalDateTime.of(2026, 10, 16, 12, 0)
        val subscription = subscription(nextBillingAt = now)

        subscription.markChargeFailed(now)
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

    @Test
    fun `해지 예약 기간 안에는 멤버십이 유효하고 기간이 지나면 무효다`() {
        val endsAt = LocalDateTime.of(2026, 11, 1, 0, 0)
        val subscription = subscription(nextBillingAt = endsAt)
        subscription.scheduleCancel(LocalDateTime.of(2026, 10, 16, 0, 0))

        assertEquals(TossBillingSubscriptionStatus.CANCEL_SCHEDULED, subscription.status)
        assertTrue(subscription.isMembershipActive(endsAt.minusDays(1)))
        assertTrue(subscription.isMembershipActive(endsAt))
        assertFalse(subscription.isMembershipActive(endsAt.plusSeconds(1)))
        assertEquals(TossBillingSubscriptionStatus.CANCELED, subscription.status)
    }

    private fun subscription(nextBillingAt: LocalDateTime): TossBillingSubscription {
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
        )
    }
}
