package com.sleekydz86.idolglow.payment.graphql

import com.sleekydz86.idolglow.payment.application.dto.PaymentMonthlyChartPoint

data class AdminPaymentMonthlyChartPointGraphQlResponse(
    val month: String,
    val totalCount: Int,
    val succeededCount: Int,
    val failedCount: Int,
    val canceledCount: Int,
) {
    companion object {
        fun from(response: PaymentMonthlyChartPoint): AdminPaymentMonthlyChartPointGraphQlResponse =
            AdminPaymentMonthlyChartPointGraphQlResponse(
                month = response.month,
                totalCount = response.totalCount.toInt(),
                succeededCount = response.succeededCount.toInt(),
                failedCount = response.failedCount.toInt(),
                canceledCount = response.canceledCount.toInt(),
            )
    }
}
