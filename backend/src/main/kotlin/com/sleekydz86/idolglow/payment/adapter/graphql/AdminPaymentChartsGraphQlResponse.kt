package com.sleekydz86.idolglow.payment.graphql

import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentChartsResponse

data class AdminPaymentChartsGraphQlResponse(
    val byStatus: List<AdminPaymentStatusChartPointGraphQlResponse>,
    val byMonth: List<AdminPaymentMonthlyChartPointGraphQlResponse>,
) {
    companion object {
        fun from(response: AdminPaymentChartsResponse): AdminPaymentChartsGraphQlResponse =
            AdminPaymentChartsGraphQlResponse(
                byStatus = response.byStatus.map(AdminPaymentStatusChartPointGraphQlResponse::from),
                byMonth = response.byMonth.map(AdminPaymentMonthlyChartPointGraphQlResponse::from),
            )
    }
}
