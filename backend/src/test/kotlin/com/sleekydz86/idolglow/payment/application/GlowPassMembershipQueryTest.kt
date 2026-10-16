package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class GlowPassMembershipQueryTest {
    private val subscriptionRepository = mockk<TossBillingSubscriptionJpaRepository>()
    private val query = GlowPassMembershipQuery(subscriptionRepository)

    @Test
    fun `ACTIVE 구독은 멤버십 유효`() {
        every { subscriptionRepository.findByUserIdAndPlanCode(1L, "MONTHLY") } returns
            subscription(TossBillingSubscriptionStatus.ACTIVE, LocalDateTime.of(2026, 11, 1, 0, 0))

        assertTrue(query.isActive(1L, LocalDateTime.of(2026, 10, 16, 0, 0)))
    }

    @Test
    fun `PAUSED 구독은 멤버십 무효`() {
        every { subscriptionRepository.findByUserIdAndPlanCode(1L, "MONTHLY") } returns
            subscription(TossBillingSubscriptionStatus.PAUSED, LocalDateTime.of(2026, 11, 1, 0, 0))

        assertFalse(query.isActive(1L, LocalDateTime.of(2026, 10, 16, 0, 0)))
    }

    private fun subscription(
        status: TossBillingSubscriptionStatus,
        nextBillingAt: LocalDateTime,
    ): TossBillingSubscription {
        val method =
            TossBillingMethod(
                userId = 1L,
                customerKey = "idolglow-user-1",
                billingKey = "bk",
            )
        return TossBillingSubscription(
            userId = 1L,
            billingMethod = method,
            orderName = "Glow Pass",
            amount = BillingDefaults.MONTHLY_AMOUNT,
            status = status,
            nextBillingAt = nextBillingAt,
        )
    }
}
