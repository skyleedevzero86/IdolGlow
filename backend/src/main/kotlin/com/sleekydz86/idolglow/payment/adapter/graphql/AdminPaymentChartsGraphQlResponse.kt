package com.sleekydz86.idolglow.payment.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentChartsResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentDetailResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentOverviewResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentSummaryResponse
import com.sleekydz86.idolglow.payment.application.dto.MyPagePaymentSummaryResponse
import com.sleekydz86.idolglow.payment.application.dto.PaymentMonthlyChartPoint
import com.sleekydz86.idolglow.payment.application.dto.PaymentStatusChartPoint

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
