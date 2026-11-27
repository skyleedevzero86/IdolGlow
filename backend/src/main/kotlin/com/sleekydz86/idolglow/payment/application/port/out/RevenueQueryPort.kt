package com.sleekydz86.idolglow.payment.application.port.out

import java.math.BigDecimal
import java.time.LocalDateTime

interface RevenueQueryPort {
    fun sumNetApprovedBetween(
        from: LocalDateTime,
        toExclusive: LocalDateTime,
    ): BigDecimal

    fun findActiveBillingAmounts(): List<BillingAmount>
}

data class BillingAmount(
    val amount: Long,
    val cycleMonths: Int,
)
