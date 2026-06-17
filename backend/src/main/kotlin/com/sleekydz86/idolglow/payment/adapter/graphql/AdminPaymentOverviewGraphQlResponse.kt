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

data class AdminPaymentOverviewGraphQlResponse(
    val totalCount: Int,
    val pendingCount: Int,
    val succeededCount: Int,
    val failedCount: Int,
    val canceledCount: Int,
    val expiredCount: Int,
    val refundedCount: Int,
    val partialCanceledCount: Int,
    val cancelableCount: Int,
    val grossAmount: String,
    val refundedAmount: String,
    val netAmount: String,
) {
    companion object {
        fun from(response: AdminPaymentOverviewResponse): AdminPaymentOverviewGraphQlResponse =
            AdminPaymentOverviewGraphQlResponse(
                totalCount = response.totalCount.toInt(),
                pendingCount = response.pendingCount.toInt(),
                succeededCount = response.succeededCount.toInt(),
                failedCount = response.failedCount.toInt(),
                canceledCount = response.canceledCount.toInt(),
                expiredCount = response.expiredCount.toInt(),
                refundedCount = response.refundedCount.toInt(),
                partialCanceledCount = response.partialCanceledCount.toInt(),
                cancelableCount = response.cancelableCount.toInt(),
                grossAmount = response.grossAmount.asGraphQlNumber(),
                refundedAmount = response.refundedAmount.asGraphQlNumber(),
                netAmount = response.netAmount.asGraphQlNumber(),
            )
    }
}
