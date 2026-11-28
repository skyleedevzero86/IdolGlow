package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.BillingCycle
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingPlanChange
import com.sleekydz86.idolglow.payment.domain.TossBillingPlanChangeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingPlanChangeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import java.time.LocalDateTime

@Component
class BillingChargeSettlement(
    private val subscriptionRepository: TossBillingSubscriptionJpaRepository,
    private val planChangeRepository: TossBillingPlanChangeJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun onDone(
        charge: TossBillingCharge,
        now: LocalDateTime,
    ) {
        charge.subscriptionId
            ?.let { subscriptionRepository.findWithBillingMethodById(it) }
            ?.let { subscription ->
                subscription.markCharged(now)
                subscription.useCredit(charge.creditUsed)
            }
        planChangeRepository.findByChargeId(charge.id)?.let { applyPlanChange(it, now) }
    }

    fun onFailed(
        charge: TossBillingCharge,
        now: LocalDateTime,
    ) {
        charge.subscriptionId
            ?.let { subscriptionRepository.findWithBillingMethodById(it) }
            ?.markChargeFailed(now)
        planChangeRepository
            .findByChargeId(charge.id)
            ?.takeIf { it.status == TossBillingPlanChangeStatus.PENDING }
            ?.markFailed()
    }

    fun applyPlanChange(
        change: TossBillingPlanChange,
        now: LocalDateTime,
    ) {
        if (change.status != TossBillingPlanChangeStatus.PENDING) {
            return
        }
        val subscription =
            subscriptionRepository.findByIdForUpdate(change.subscriptionId)
                ?: throw IllegalStateException("이용권 변경 대상 구독이 없습니다. changeId=${change.id}")
        if (subscription.nextBillingAt != change.periodEndAt) {
            log.error(
                "이용권 변경 중 다음 결제일이 바뀌었습니다. changeId={} subscriptionId={} expected={} actual={}",
                change.id,
                subscription.id,
                change.periodEndAt,
                subscription.nextBillingAt,
            )
        }
        apply(subscription, change, now)
    }

    private fun apply(
        subscription: TossBillingSubscription,
        change: TossBillingPlanChange,
        now: LocalDateTime,
    ) {
        val cycle = BillingCycle.ofMonths(change.toCycleMonths)
        subscription.useCredit(change.creditUsed)
        subscription.addCredit(change.creditAdded)
        subscription.changePlan(cycle, planOrderName(cycle), now, charged = change.cardAmount > 0)
        change.markDone()
        log.info(
            "이용권을 바꿨습니다. changeId={} subscriptionId={} from={} to={} cardAmount={} creditAdded={}",
            change.id,
            subscription.id,
            change.fromCycleMonths,
            change.toCycleMonths,
            change.cardAmount,
            change.creditAdded,
        )
    }

    companion object {
        fun planOrderName(cycle: BillingCycle): String = "IdolGlow Glow Pass ${cycle.label}"
    }
}
