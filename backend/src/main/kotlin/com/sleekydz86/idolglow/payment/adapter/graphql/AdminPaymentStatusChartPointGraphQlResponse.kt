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
