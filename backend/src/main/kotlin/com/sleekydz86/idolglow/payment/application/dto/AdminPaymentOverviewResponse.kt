package com.sleekydz86.idolglow.payment.application.dto

import java.math.BigDecimal

data class AdminPaymentOverviewResponse(
    val totalCount: Long,
    val pendingCount: Long,
    val succeededCount: Long,
    val failedCount: Long,
    val canceledCount: Long,
    val expiredCount: Long,
    val refundedCount: Long,
    val partialCanceledCount: Long,
    val cancelableCount: Long,
    val grossAmount: BigDecimal,
    val refundedAmount: BigDecimal,
    val netAmount: BigDecimal,
)
