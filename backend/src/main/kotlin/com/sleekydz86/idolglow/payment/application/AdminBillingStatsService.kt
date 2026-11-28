package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.application.dto.AdminBillingStatsResponse
import com.sleekydz86.idolglow.payment.application.dto.BillingCycleCount
import com.sleekydz86.idolglow.payment.application.dto.RenewalNoticeStats
import com.sleekydz86.idolglow.payment.domain.BillingCycle
import com.sleekydz86.idolglow.payment.domain.RenewalDecision
import com.sleekydz86.idolglow.payment.domain.TossBillingRenewalNotice
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscriptionStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingRenewalNoticeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Transactional(readOnly = true)
@Service
class AdminBillingStatsService(
    private val subscriptionRepository: TossBillingSubscriptionJpaRepository,
    private val renewalNoticeRepository: TossBillingRenewalNoticeJpaRepository,
) {
    fun stats(now: LocalDateTime = LocalDateTime.now()): AdminBillingStatsResponse {
        val subscriptions = subscriptionRepository.findAllByStatusIn(COUNTED_STATUSES)
        val subscribers = subscriptions.filter { it.status != TossBillingSubscriptionStatus.PAUSED }
        return AdminBillingStatsResponse(
            activeCount = subscriptions.countOf { it.status == ACTIVE && it.autoRenew },
            cancelScheduledCount = subscriptions.countOf { it.isThisPeriodOnly() },
            pausedCount = subscriptions.countOf { it.status == TossBillingSubscriptionStatus.PAUSED },
            retryingCount = subscriptions.countOf { it.status == ACTIVE && it.failCount > 0 },
            byCycle = cycleCounts(subscribers),
            monthlyRevenue = subscribers.sumOf { it.amount / it.cycleMonths.coerceAtLeast(1) },
            renewalNotice = renewalStats(now),
        )
    }

    private fun cycleCounts(subscribers: List<TossBillingSubscription>): List<BillingCycleCount> =
        BillingCycle.entries.map { cycle ->
            BillingCycleCount(cycle.months, subscribers.countOf { it.cycleMonths == cycle.months })
        }

    private fun renewalStats(now: LocalDateTime): RenewalNoticeStats {
        val notices = renewalNoticeRepository.findAllBySentAtBetween(now.minusDays(RENEWAL_WINDOW_DAYS), now)
        return RenewalNoticeStats(
            sent = notices.size.toLong(),
            continued = notices.countOf { it.decision == RenewalDecision.CONTINUE },
            stopped = notices.countOf { it.decision == RenewalDecision.STOP },
            noAnswer = notices.countOf { it.decision == null },
        )
    }

    private fun TossBillingSubscription.isThisPeriodOnly(): Boolean =
        status == TossBillingSubscriptionStatus.CANCEL_SCHEDULED || (status == ACTIVE && !autoRenew)

    private fun <T> List<T>.countOf(predicate: (T) -> Boolean): Long = count(predicate).toLong()

    private companion object {
        val ACTIVE = TossBillingSubscriptionStatus.ACTIVE
        val COUNTED_STATUSES =
            listOf(
                TossBillingSubscriptionStatus.ACTIVE,
                TossBillingSubscriptionStatus.CANCEL_SCHEDULED,
                TossBillingSubscriptionStatus.PAUSED,
            )
        const val RENEWAL_WINDOW_DAYS = 7L
    }
}
