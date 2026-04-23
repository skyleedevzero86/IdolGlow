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

data class AdminPaymentSummaryGraphQlResponse(
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
    val visitDate: String,
    val visitStartTime: String,
    val visitEndTime: String,
) {
    companion object {
        fun from(response: AdminPaymentSummaryResponse): AdminPaymentSummaryGraphQlResponse =
            AdminPaymentSummaryGraphQlResponse(
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
                visitDate = requireNotNull(response.visitDate.asGraphQlValue()),
                visitStartTime = requireNotNull(response.visitStartTime.asGraphQlValue()),
                visitEndTime = requireNotNull(response.visitEndTime.asGraphQlValue()),
            )
    }
}

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

data class MyPaymentSummaryGraphQlResponse(
    val paymentId: String,
    val reservationId: String,
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
    val canceledAt: String?,
    val visitDate: String,
    val visitStartTime: String,
    val visitEndTime: String,
    val canCancel: Boolean,
    val cancelDeadlineAt: String?,
    val receiptAvailable: Boolean,
) {
    companion object {
        fun from(response: MyPagePaymentSummaryResponse): MyPaymentSummaryGraphQlResponse =
            MyPaymentSummaryGraphQlResponse(
                paymentId = response.paymentId.asGraphQlId(),
                reservationId = response.reservationId.asGraphQlId(),
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
                canceledAt = response.canceledAt.asGraphQlValue(),
                visitDate = requireNotNull(response.visitDate.asGraphQlValue()),
                visitStartTime = requireNotNull(response.visitStartTime.asGraphQlValue()),
                visitEndTime = requireNotNull(response.visitEndTime.asGraphQlValue()),
                canCancel = response.canCancel,
                cancelDeadlineAt = response.cancelDeadlineAt.asGraphQlValue(),
                receiptAvailable = response.receiptAvailable,
            )
    }
}
