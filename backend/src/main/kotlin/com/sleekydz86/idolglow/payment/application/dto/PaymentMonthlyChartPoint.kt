package com.sleekydz86.idolglow.payment.application.dto

data class PaymentMonthlyChartPoint(
    val month: String,
    val totalCount: Long,
    val succeededCount: Long,
    val failedCount: Long,
    val canceledCount: Long,
)
