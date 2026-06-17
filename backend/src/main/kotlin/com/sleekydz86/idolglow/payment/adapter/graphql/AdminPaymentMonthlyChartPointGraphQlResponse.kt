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
