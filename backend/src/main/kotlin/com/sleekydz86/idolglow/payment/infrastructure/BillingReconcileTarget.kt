package com.sleekydz86.idolglow.payment.infrastructure

import java.time.LocalDateTime

data class BillingReconcileTarget(
    val attemptId: Long,
    val chargeId: Long,
    val tossOrderId: String,
    val createdAt: LocalDateTime,
)
