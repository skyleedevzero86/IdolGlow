package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentRefund
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.domain.RefundRequestedBy
import com.sleekydz86.idolglow.payment.infrastructure.PaymentRefundJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode

@Service
class PaymentRefundService(
    private val paymentRefundTxService: PaymentRefundTxService,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val paymentRefundJpaRepository: PaymentRefundJpaRepository,
) {
    fun refundBeforeReservationCancel(
        payment: Payment,
        reservation: Reservation,
        cancelReason: String,
        requestedBy: RefundRequestedBy,
    ): RefundPrepareResult {
        require(reservation.status == ReservationStatus.BOOKED) { "확정된 예약만 환불 대상입니다." }
        require(payment.status == PaymentStatus.SUCCEEDED) { "승인된 결제만 환불할 수 있습니다." }
        return paymentRefundTxService.prepareRefund(payment.id, cancelReason, requestedBy)
    }

    fun refundAfterExpiredReservation(
        payment: Payment,
        reservation: Reservation,
    ): RefundPrepareResult {
        require(payment.reservation.id == reservation.id)
        return paymentRefundTxService.prepareRefund(payment.id, "결제 시간 초과로 자동 환불", RefundRequestedBy.SYSTEM)
    }

    fun cancelPrepared(prepared: RefundPrepareResult) {
        if (!prepared.needsTossCancel) {
            return
        }
        cancelWithToss(prepared)
    }

    fun settlePreparedRefund(paymentId: Long): Payment {
        val prepared = paymentRefundTxService.findPendingTossRefund(paymentId)
        if (prepared != null) {
            cancelWithToss(prepared)
        }
        return paymentRefundTxService.loadPayment(paymentId)
    }

    @Transactional(readOnly = true)
    fun findRefundsByPaymentId(paymentId: Long): List<PaymentRefund> =
        paymentRefundJpaRepository.findAllByPaymentIdOrderByCreatedAtDesc(paymentId)

    fun adminRetryLastFailedRefund(paymentId: Long): PaymentRefund {
        val prepared = paymentRefundTxService.prepareAdminRetry(paymentId)
        val refundId =
            prepared.refundId ?: throw IllegalStateException("환불 이력이 없습니다. paymentId=$paymentId")
        applyAdminRetry(prepared, refundId)
        return paymentRefundJpaRepository.findById(refundId).orElseThrow {
            IllegalArgumentException("환불 이력을 찾을 수 없습니다: $refundId")
        }
    }

    private fun applyAdminRetry(
        prepared: RefundPrepareResult,
        refundId: Long,
    ) {
        val paymentKey = prepared.paymentKey ?: throw IllegalStateException("토스 paymentKey 가 없어 환불할 수 없습니다.")
        val remote = tossPaymentsApiClient.getPayment(paymentKey)
        val json = remote.json
        if (remote.isSuccess2xx && json != null && fullyCanceled(prepared, json)) {
            paymentRefundTxService.completeRefund(refundId, remote)
            return
        }
        if (remote.isSuccess2xx && json != null && json.path("status").asText(null)?.trim() == "DONE") {
            cancelWithToss(prepared)
        }
    }

    private fun fullyCanceled(
        prepared: RefundPrepareResult,
        json: JsonNode,
    ): Boolean {
        val amount = paymentRefundTxService.loadPayment(prepared.paymentId).amount
        return TossPaymentResponseMapper.isFullyCanceled(json, amount)
    }

    private fun cancelWithToss(prepared: RefundPrepareResult) {
        val paymentKey = prepared.paymentKey ?: throw IllegalStateException("토스 paymentKey 가 없어 환불할 수 없습니다.")
        val refundId = prepared.refundId ?: throw IllegalStateException("환불 이력이 없습니다. paymentId=${prepared.paymentId}")
        val reason = prepared.cancelReason ?: "결제 취소"
        paymentLogCommandService.append(
            payment = null,
            orderId = prepared.orderId,
            paymentKey = paymentKey,
            logType = PaymentLogType.CANCEL_REQUEST,
            step = PaymentLogStep.SERVER,
            requestUrl = "/v1/payments/$paymentKey/cancel",
            httpMethod = "POST",
            requestBody = """{"cancelReason":"${reason.replace("\"", "'")}"}""",
            linkedPaymentId = prepared.paymentId,
        )
        val response =
            tossPaymentsApiClient.cancel(
                paymentKey = paymentKey,
                cancelReason = reason,
                cancelAmount = null,
                idempotencyKey = prepared.idempotencyKey,
            )
        paymentLogCommandService.append(
            payment = null,
            orderId = prepared.orderId,
            paymentKey = paymentKey,
            logType = if (response.isSuccess2xx) PaymentLogType.CANCEL_RESPONSE else PaymentLogType.CANCEL_ERROR,
            step = PaymentLogStep.TOSS_API,
            httpStatus = response.httpStatus,
            responseBody = response.rawBody,
            errorMessage = response.error?.message,
            linkedPaymentId = prepared.paymentId,
        )
        if (response.isInconclusive) {
            return
        }
        paymentRefundTxService.completeRefund(refundId, response)
    }
}
