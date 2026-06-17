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

data class AdminPaymentDetailGraphQlResponse(
    val paymentId: String,
    val reservationId: String,
    val userId: String,
    val productId: String,
    val productName: String,
    val provider: String,
    val paymentReference: String,
    val amount: String,
    val cancelAmount: String,
    val status: String,
    val failureReason: String?,
    val approvedAt: String?,
    val failedAt: String?,
    val expiredAt: String?,
    val canceledAt: String?,
    val visitDate: String,
    val visitStartTime: String,
    val visitEndTime: String,
    val canCancel: Boolean,
    val receiptAvailable: Boolean,
) {
    companion object {
        fun from(response: AdminPaymentDetailResponse): AdminPaymentDetailGraphQlResponse =
            AdminPaymentDetailGraphQlResponse(
                paymentId = response.paymentId.asGraphQlId(),
                reservationId = response.reservationId.asGraphQlId(),
                userId = response.userId.asGraphQlId(),
                productId = response.productId.asGraphQlId(),
                productName = response.productName,
                provider = response.provider.name,
                paymentReference = response.paymentReference,
                amount = response.amount.asGraphQlNumber(),
                cancelAmount = response.cancelAmount.asGraphQlNumber(),
                status = response.status.name,
                failureReason = response.failureReason,
                approvedAt = response.approvedAt.asGraphQlValue(),
                failedAt = response.failedAt.asGraphQlValue(),
                expiredAt = response.expiredAt.asGraphQlValue(),
                canceledAt = response.canceledAt.asGraphQlValue(),
                visitDate = requireNotNull(response.visitDate.asGraphQlValue()),
                visitStartTime = requireNotNull(response.visitStartTime.asGraphQlValue()),
                visitEndTime = requireNotNull(response.visitEndTime.asGraphQlValue()),
                canCancel = response.canCancel,
                receiptAvailable = response.receiptAvailable,
            )
    }
}
