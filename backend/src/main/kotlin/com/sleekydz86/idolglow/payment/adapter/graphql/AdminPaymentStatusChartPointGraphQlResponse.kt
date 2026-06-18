package com.sleekydz86.idolglow.payment.graphql

import com.sleekydz86.idolglow.payment.application.dto.PaymentStatusChartPoint

data class AdminPaymentStatusChartPointGraphQlResponse(
    val status: String,
    val count: Int,
) {
    companion object {
        fun from(response: PaymentStatusChartPoint): AdminPaymentStatusChartPointGraphQlResponse =
            AdminPaymentStatusChartPointGraphQlResponse(
                status = response.status,
                count = response.count.toInt(),
            )
    }
}
