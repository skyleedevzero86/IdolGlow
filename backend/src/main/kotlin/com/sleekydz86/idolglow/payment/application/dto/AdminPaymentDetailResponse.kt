package com.sleekydz86.idolglow.payment.application.dto

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentProvider
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AdminPaymentDetailResponse(
    val paymentId: Long,
    val reservationId: Long,
    val userId: Long,
    val productId: Long,
    val productName: String,
    val provider: PaymentProvider,
    val paymentReference: String,
    val paymentNo: String,
    val orderId: String,
    val paymentKey: String?,
    val amount: BigDecimal,
    val cancelAmount: BigDecimal,
    val status: PaymentStatus,
    val orderName: String?,
    val currency: String?,
    val gatewayMethod: String?,
    val gatewayType: String?,
    val externalStatus: String?,
    val failureReason: String?,
    val failCode: String?,
    val approvedAt: LocalDateTime?,
    val failedAt: LocalDateTime?,
    val expiredAt: LocalDateTime?,
    val canceledAt: LocalDateTime?,
    val cardCompany: String?,
    val cardNumber: String?,
    val easyPayProvider: String?,
    val virtualAccountBank: String?,
    val virtualAccountNumber: String?,
    val virtualAccountDueDate: LocalDateTime?,
    val visitDate: LocalDate,
    val visitStartTime: LocalTime,
    val visitEndTime: LocalTime,
    val canCancel: Boolean,
    val receiptAvailable: Boolean,
    val refunds: List<PaymentRefundResponse>,
    val logs: List<AdminPaymentLogResponse>,
) {
    companion object {
        fun from(
            payment: Payment,
            canCancel: Boolean,
            receiptAvailable: Boolean,
            refunds: List<PaymentRefundResponse>,
            logs: List<AdminPaymentLogResponse>,
        ): AdminPaymentDetailResponse =
            AdminPaymentDetailResponse(
                paymentId = payment.id,
                reservationId = payment.reservation.id,
                userId = payment.reservation.userId,
                productId = payment.reservation.reservationSlot.product.id,
                productName = payment.reservation.reservationSlot.product.name,
                provider = payment.provider,
                paymentReference = payment.paymentReference,
                paymentNo = payment.paymentNo,
                orderId = payment.orderId,
                paymentKey = payment.paymentKey,
                amount = payment.amount,
                cancelAmount = payment.cancelAmount,
                status = payment.status,
                orderName = payment.orderName,
                currency = payment.currency,
                gatewayMethod = payment.gatewayMethod,
                gatewayType = payment.gatewayType,
                externalStatus = payment.externalStatus,
                failureReason = payment.failureReason,
                failCode = payment.failCode,
                approvedAt = payment.approvedAt,
                failedAt = payment.failedAt,
                expiredAt = payment.expiredAt,
                canceledAt = payment.canceledAt,
                cardCompany = payment.cardCompany,
                cardNumber = payment.cardNumber,
                easyPayProvider = payment.easyPayProvider,
                virtualAccountBank = payment.virtualAccountBank,
                virtualAccountNumber = payment.virtualAccountNumber,
                virtualAccountDueDate = payment.virtualAccountDueDate,
                visitDate = payment.reservation.visitDate,
                visitStartTime = payment.reservation.visitStartTime,
                visitEndTime = payment.reservation.visitEndTime,
                canCancel = canCancel,
                receiptAvailable = receiptAvailable,
                refunds = refunds,
                logs = logs,
            )
    }
}
