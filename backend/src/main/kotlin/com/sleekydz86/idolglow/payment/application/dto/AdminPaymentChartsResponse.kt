package com.sleekydz86.idolglow.payment.application.dto

data class AdminPaymentChartsResponse(
    val byStatus: List<PaymentStatusChartPoint>,
    val byMonth: List<PaymentMonthlyChartPoint>,
)
