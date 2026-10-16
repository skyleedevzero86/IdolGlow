package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class GlowPassMembershipQuery(
    private val subscriptionRepository: TossBillingSubscriptionJpaRepository,
) : MembershipQuery {
    @Transactional
    override fun isActive(
        userId: Long,
        at: LocalDateTime,
    ): Boolean {
        val subscription = subscriptionRepository.findByUserIdAndPlanCode(userId, PLAN_MONTHLY) ?: return false
        val active = subscription.isMembershipActive(at)
        return active
    }

    companion object {
        private const val PLAN_MONTHLY = "MONTHLY"
    }
}
