package com.sleekydz86.idolglow.payment.domain

import java.time.LocalDate
import java.time.temporal.ChronoUnit

data class PlanChangeQuote(
    val periodDays: Int,
    val remainingDays: Int,
    val remainingValue: Long,
    val newAmount: Long,
    val creditUsed: Long,
    val cardAmount: Long,
    val creditAdded: Long,
)

object BillingPlanChangeCalculator {
    const val MIN_CARD_AMOUNT = 100L

    fun remainingDays(
        today: LocalDate,
        periodEnd: LocalDate,
    ): Int = maxOf(0L, ChronoUnit.DAYS.between(today, periodEnd) - 1).toInt()

    fun quote(
        periodStart: LocalDate,
        periodEnd: LocalDate,
        today: LocalDate,
        periodValue: Long,
        newAmount: Long,
        creditBalance: Long,
    ): PlanChangeQuote {
        val periodDays = maxOf(1L, ChronoUnit.DAYS.between(periodStart, periodEnd)).toInt()
        val remaining = minOf(remainingDays(today, periodEnd), periodDays)
        val remainingValue = periodValue * remaining / periodDays
        val settle = newAmount - remainingValue
        if (settle <= 0) {
            return PlanChangeQuote(periodDays, remaining, remainingValue, newAmount, 0L, 0L, -settle)
        }
        val creditUsed = minOf(creditBalance, settle)
        return PlanChangeQuote(
            periodDays = periodDays,
            remainingDays = remaining,
            remainingValue = remainingValue,
            newAmount = newAmount,
            creditUsed = creditUsed,
            cardAmount = chargeableAmount(settle - creditUsed),
            creditAdded = 0L,
        )
    }

    fun chargeableAmount(amount: Long): Long = if (amount < MIN_CARD_AMOUNT) 0L else amount
}
