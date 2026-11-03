package com.sleekydz86.idolglow.payment.domain

enum class BillingCycle(
    val months: Int,
    val amount: Long,
) {
    MONTHLY(months = 1, amount = 4_900L),
    QUARTERLY(months = 3, amount = 13_900L),
    HALF_YEARLY(months = 6, amount = 25_900L),
    YEARLY(months = 12, amount = 47_000L),
    ;

    companion object {
        fun ofMonths(months: Int): BillingCycle =
            entries.firstOrNull { it.months == months }
                ?: throw IllegalArgumentException("지원하지 않는 결제 주기입니다. cycleMonths=$months")
    }
}
