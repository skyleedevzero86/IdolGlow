package com.sleekydz86.idolglow.payment.domain

enum class BillingCycle(
    val months: Int,
    val amount: Long,
    val label: String,
) {
    MONTHLY(months = 1, amount = 4_900L, label = "1개월"),
    QUARTERLY(months = 3, amount = 13_900L, label = "3개월"),
    HALF_YEARLY(months = 6, amount = 25_900L, label = "6개월"),
    YEARLY(months = 12, amount = 47_000L, label = "1년"),
    ;

    companion object {
        fun ofMonths(months: Int): BillingCycle =
            entries.firstOrNull { it.months == months }
                ?: throw IllegalArgumentException("지원하지 않는 결제 주기입니다. cycleMonths=$months")
    }
}
