package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.application.PlanChangeCommand
import com.sleekydz86.idolglow.payment.domain.BillingCycle
import com.sleekydz86.idolglow.payment.domain.PlanChangeQuote
import com.sleekydz86.idolglow.payment.domain.TossBillingPlanChange
import com.sleekydz86.idolglow.payment.domain.TossBillingPlanChangeStatus
import com.sleekydz86.idolglow.payment.domain.TossBillingSubscription
import jakarta.validation.constraints.PositiveOrZero
import java.time.LocalDateTime

data class PlanChangeRequest(
    val cycleMonths: Int,
    @field:PositiveOrZero
    val expectedCardAmount: Long,
    @field:PositiveOrZero
    val expectedCreditAdded: Long,
) {
    fun toCommand() = PlanChangeCommand(cycleMonths, expectedCardAmount, expectedCreditAdded)
}

data class PlanChangePreviewResponse(
    val currentCycleMonths: Int,
    val newCycleMonths: Int,
    val periodDays: Int,
    val remainingDays: Int,
    val remainingValue: Long,
    val newAmount: Long,
    val creditBalance: Long,
    val creditUsed: Long,
    val cardAmount: Long,
    val creditAdded: Long,
    val nextBillingAt: LocalDateTime,
) {
    companion object {
        fun of(
            subscription: TossBillingSubscription,
            cycle: BillingCycle,
            quote: PlanChangeQuote,
            now: LocalDateTime,
        ) = PlanChangePreviewResponse(
            currentCycleMonths = subscription.cycleMonths,
            newCycleMonths = cycle.months,
            periodDays = quote.periodDays,
            remainingDays = quote.remainingDays,
            remainingValue = quote.remainingValue,
            newAmount = quote.newAmount,
            creditBalance = subscription.creditAmount,
            creditUsed = quote.creditUsed,
            cardAmount = quote.cardAmount,
            creditAdded = quote.creditAdded,
            nextBillingAt = now.plusMonths(cycle.months.toLong()),
        )
    }
}

data class PlanChangeResultResponse(
    val status: TossBillingPlanChangeStatus,
    val cardAmount: Long,
    val creditAdded: Long,
    val subscription: TossBillingSubscriptionResponse,
) {
    companion object {
        fun of(
            change: TossBillingPlanChange,
            subscription: TossBillingSubscription,
        ) = PlanChangeResultResponse(
            status = change.status,
            cardAmount = change.cardAmount,
            creditAdded = change.creditAdded,
            subscription = TossBillingSubscriptionResponse.from(subscription),
        )
    }
}
