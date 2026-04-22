package com.sleekydz86.idolglow.payment.application.dto

data class AdminPaymentChartsResponse(
    val byStatus: List<PaymentStatusChartPoint>,
    val byMonth: List<PaymentMonthlyChartPoint>,
)

data class PaymentStatusChartPoint(
    val status: String,
    val count: Long,
)

data class PaymentMonthlyChartPoint(
    val month: String,
    val totalCount: Long,
    val succeededCount: Long,
    val failedCount: Long,
    val canceledCount: Long,
)
