package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.TossBillingMethod
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

@DisplayName("Glow Pass Membership Query")
class GlowPassMembershipQueryTest {
    private val subscriptionRepository = mockk<TossBillingSubscriptionJpaRepository>()
    private val query = GlowPassMembershipQuery(subscriptionRepository)

    @DisplayName("ACTIVE 구독은 멤버십 유효")
    @Test
    fun activeSubscriptionIsMember() {
        // given
        every { subscriptionRepository.findByUserIdAndPlanCode(1L, "MONTHLY") } returns
        // when
            subscription(TossBillingSubscriptionStatus.ACTIVE, LocalDateTime.of(2026, 11, 1, 0, 0))

        // then
        assertTrue(query.isActive(1L, LocalDateTime.of(2026, 10, 16, 0, 0)))
    }

    @DisplayName("PAUSED 구독은 멤버십 무효")
    @Test
    fun pausedSubscriptionIsNotMember() {
        // given
        every { subscriptionRepository.findByUserIdAndPlanCode(1L, "MONTHLY") } returns
        // when
            subscription(TossBillingSubscriptionStatus.PAUSED, LocalDateTime.of(2026, 11, 1, 0, 0))

        // then
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
