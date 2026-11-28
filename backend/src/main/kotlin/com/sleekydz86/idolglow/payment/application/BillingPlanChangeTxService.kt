package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.billing.BillingPlanChangeExceptionType
import com.sleekydz86.idolglow.payment.application.dto.PlanChangePreviewResponse
import com.sleekydz86.idolglow.payment.application.dto.PlanChangeResultResponse
import com.sleekydz86.idolglow.payment.domain.BillingCycle
import com.sleekydz86.idolglow.payment.domain.BillingPlanChangeCalculator
import com.sleekydz86.idolglow.payment.domain.PlanChangeBlock
import com.sleekydz86.idolglow.payment.domain.PlanChangeQuote
import com.sleekydz86.idolglow.payment.domain.TossBillingCharge
import com.sleekydz86.idolglow.payment.domain.TossBillingChargeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingPlanChange
import com.sleekydz86.idolglow.payment.domain.TossBillingPlanChangeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingChargeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingPlanChangeJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossBillingSubscriptionJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
class BillingPlanChangeTxService(
    private val subscriptionRepository: TossBillingSubscriptionJpaRepository,
    private val chargeRepository: TossBillingChargeJpaRepository,
    private val planChangeRepository: TossBillingPlanChangeJpaRepository,
    private val settlement: BillingChargeSettlement,
) {
    @Transactional(readOnly = true)
    fun preview(
        userId: Long,
        cycleMonths: Int,
        now: LocalDateTime,
    ): PlanChangePreviewResponse {
        val cycle = targetCycle(cycleMonths)
        val subscription =
            subscriptionRepository.findByUserIdAndPlanCode(userId, TossBillingService.PLAN_MONTHLY)
                ?: throw CustomException(BillingPlanChangeExceptionType.SUBSCRIPTION_NOT_FOUND)
        assertChangeable(subscription, cycle, now)
        return PlanChangePreviewResponse.of(subscription, cycle, quoteOf(subscription, cycle, now), now)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun prepare(
        userId: Long,
        request: PlanChangeCommand,
        now: LocalDateTime,
    ): PreparedPlanChange {
        val cycle = targetCycle(request.cycleMonths)
        val subscription = lockedSubscription(userId)
        assertChangeable(subscription, cycle, now)
        val quote = quoteOf(subscription, cycle, now)
        assertQuote(subscription, quote, request)
        val change = recordChange(subscription, cycle, quote)
        if (quote.cardAmount == 0L) {
            settlement.applyPlanChange(change, now)
            return PreparedPlanChange(changeId = change.id, chargeId = null)
        }
        val charge = chargeRepository.save(planChangeCharge(subscription, change, cycle, now))
        change.attachCharge(charge.id)
        return PreparedPlanChange(changeId = change.id, chargeId = charge.id)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun result(changeId: Long): PlanChangeResultResponse {
        val change =
            planChangeRepository.findById(changeId).orElseThrow {
                IllegalStateException("이용권 변경 기록이 없습니다. changeId=$changeId")
            }
        val subscription =
            subscriptionRepository.findWithBillingMethodById(change.subscriptionId)
                ?: throw IllegalStateException("이용권 변경 대상 구독이 없습니다. changeId=$changeId")
        return PlanChangeResultResponse.of(change, subscription)
    }

    private fun lockedSubscription(userId: Long): TossBillingSubscription =
        subscriptionRepository.findByUserIdAndPlanCodeForUpdate(userId, TossBillingService.PLAN_MONTHLY)
            ?: throw CustomException(BillingPlanChangeExceptionType.SUBSCRIPTION_NOT_FOUND)

    private fun assertQuote(
        subscription: TossBillingSubscription,
        quote: PlanChangeQuote,
        request: PlanChangeCommand,
    ) {
        if (quote.cardAmount != request.expectedCardAmount || quote.creditAdded != request.expectedCreditAdded) {
            throw CustomException(BillingPlanChangeExceptionType.AMOUNT_CHANGED)
        }
        if (quote.cardAmount > 0 && !subscription.billingMethod.usableForCharge()) {
            throw CustomException(BillingPlanChangeExceptionType.METHOD_UNUSABLE)
        }
    }

    private fun targetCycle(cycleMonths: Int): BillingCycle {
        val cycle =
            BillingCycle.entries.firstOrNull { it.months == cycleMonths }
                ?: throw CustomException(BillingPlanChangeExceptionType.INVALID_CYCLE)
        if (cycle == BillingCycle.MONTHLY) {
            throw CustomException(BillingPlanChangeExceptionType.INVALID_CYCLE)
        }
        return cycle
    }

    private fun assertChangeable(
        subscription: TossBillingSubscription,
        cycle: BillingCycle,
        now: LocalDateTime,
    ) {
        val block = subscription.planChangeBlock(cycle.months, now.toLocalDate())
        if (block != null) {
            throw CustomException(exceptionOf(block))
        }
        val pendingCharge =
            chargeRepository.existsBySubscriptionIdAndStatus(subscription.id, TossBillingChargeStatus.PENDING)
        val pendingChange =
            planChangeRepository.existsBySubscriptionIdAndStatus(subscription.id, TossBillingPlanChangeStatus.PENDING)
        if (pendingCharge || pendingChange) {
            throw CustomException(BillingPlanChangeExceptionType.CHARGE_PENDING)
        }
    }

    private fun quoteOf(
        subscription: TossBillingSubscription,
        cycle: BillingCycle,
        now: LocalDateTime,
    ): PlanChangeQuote {
        val end = requireNotNull(subscription.nextBillingAt) { "다음 결제일이 없습니다. subscriptionId=${subscription.id}" }
        return BillingPlanChangeCalculator.quote(
            periodStart = end.minusMonths(subscription.cycleMonths.toLong()).toLocalDate(),
            periodEnd = end.toLocalDate(),
            today = now.toLocalDate(),
            periodValue = subscription.amount,
            newAmount = cycle.amount,
            creditBalance = subscription.creditAmount,
        )
    }

    private fun recordChange(
        subscription: TossBillingSubscription,
        cycle: BillingCycle,
        quote: PlanChangeQuote,
    ): TossBillingPlanChange {
        val periodEnd = requireNotNull(subscription.nextBillingAt)
        val existing =
            planChangeRepository.findBySubscriptionIdAndPeriodEndAtAndToCycleMonths(
                subscription.id,
                periodEnd,
                cycle.months,
            )
        if (existing != null) {
            existing.restart(quote)
            return existing
        }
        return planChangeRepository.saveAndFlush(
            TossBillingPlanChange(
                userId = subscription.userId,
                subscriptionId = subscription.id,
                fromCycleMonths = subscription.cycleMonths,
                toCycleMonths = cycle.months,
                periodEndAt = periodEnd,
                quote = quote,
            ),
        )
    }

    private fun planChangeCharge(
        subscription: TossBillingSubscription,
        change: TossBillingPlanChange,
        cycle: BillingCycle,
        now: LocalDateTime,
    ): TossBillingCharge =
        TossBillingCharge(
            userId = subscription.userId,
            subscriptionId = null,
            billingMethod = subscription.billingMethod,
            orderId = "chg-${subscription.id}-${change.id}-${now.format(BillingChargeExecutor.PERIOD_FORMAT)}",
            orderName = "${BillingChargeSettlement.planOrderName(cycle)} 변경 차액",
            amount = change.cardAmount,
            status = TossBillingChargeStatus.PENDING,
        )

    private fun exceptionOf(block: PlanChangeBlock): BillingPlanChangeExceptionType =
        when (block) {
            PlanChangeBlock.NOT_ACTIVE, PlanChangeBlock.HAS_FAILURE -> BillingPlanChangeExceptionType.NOT_ALLOWED
            PlanChangeBlock.NOT_CHARGED_YET -> BillingPlanChangeExceptionType.NOT_CHARGED_YET
            PlanChangeBlock.SAME_CYCLE -> BillingPlanChangeExceptionType.SAME_CYCLE
            PlanChangeBlock.TOO_CLOSE -> BillingPlanChangeExceptionType.TOO_CLOSE
        }
}

data class PlanChangeCommand(
    val cycleMonths: Int,
    val expectedCardAmount: Long,
    val expectedCreditAdded: Long,
)

data class PreparedPlanChange(
    val changeId: Long,
    val chargeId: Long?,
)
