package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentProvider
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.PaymentLogJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime

@Service
class TossWebhookTxService(
    private val reservationPaymentLock: ReservationPaymentLock,
    private val reservationPaymentService: ReservationPaymentService,
    private val paymentRefundTxService: PaymentRefundTxService,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val paymentLogJpaRepository: PaymentLogJpaRepository,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun apply(
        orderId: String,
        paymentKey: String,
        remote: TossApiResponse,
        rawBody: String,
    ) {
        val payment = reservationPaymentLock.lockByOrderId(orderId)
        if (!isTerminalOrMissing(payment, orderId, paymentKey, rawBody) && payment != null) {
            dispatch(payment, remote, orderId, paymentKey)
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    fun isTerminalOrMissing(
        payment: Payment?,
        orderId: String,
        paymentKey: String,
        body: String,
    ): Boolean =
        when {
            payment == null -> {
                logSystem(null, orderId, paymentKey, body, "orderId에 해당하는 결제 없음")
                true
            }
            payment.provider != PaymentProvider.TOSS -> {
                logSystem(payment, orderId, paymentKey, body, "TOSS 결제가 아님")
                true
            }
            else -> closedStatus(payment, orderId, paymentKey, body)
        }

    private fun closedStatus(
        payment: Payment,
        orderId: String,
        paymentKey: String,
        body: String,
    ): Boolean =
        when (payment.status) {
            PaymentStatus.SUCCEEDED -> {
                logSystem(payment, orderId, paymentKey, body, "이미 승인됨(멱등)")
                true
            }
            PaymentStatus.CANCELED,
            PaymentStatus.EXPIRED,
            PaymentStatus.REFUNDED,
            PaymentStatus.PARTIAL_CANCELED,
            -> {
                logSystem(payment, orderId, paymentKey, body, "종료된 결제 상태: ${payment.status}")
                true
            }
            PaymentStatus.FAILED,
            PaymentStatus.REFUNDING,
            PaymentStatus.PENDING,
            PaymentStatus.CONFIRMING,
            -> false
        }

    private fun dispatch(
        payment: Payment,
        remote: TossApiResponse,
        orderId: String,
        paymentKey: String,
    ) {
        val json = remote.json
        val remoteOrderId = json?.path("orderId")?.asText(null)
        if (json == null || remoteOrderId != payment.orderId) {
            if (json != null) {
                logSystem(payment, orderId, paymentKey, remote.rawBody, "orderId 불일치")
            }
            return
        }
        when (payment.status) {
            PaymentStatus.REFUNDING -> finishRefundingFromRemote(payment, json, remote)
            PaymentStatus.FAILED -> flagLateApproval(payment, json, remote)
            else -> applyGatewayStatus(payment, json, remote, orderId, paymentKey)
        }
    }

    private fun applyGatewayStatus(
        payment: Payment,
        json: JsonNode,
        remote: TossApiResponse,
        orderId: String,
        paymentKey: String,
    ) {
        val status = json.path("status").asText(null)?.trim() ?: ""
        when (status) {
            "DONE" -> applyDoneFromRemote(payment, json, remote.rawBody, orderId, paymentKey)
            "CANCELED" ->
                reservationPaymentService.handlePaymentCanceled(
                    payment.paymentReference,
                    "결제 취소",
                )
            "ABORTED" ->
                reservationPaymentService.handlePaymentCanceled(
                    payment.paymentReference,
                    "결제 중단",
                )
            "EXPIRED" ->
                reservationPaymentService.handlePaymentFailed(
                    payment.paymentReference,
                    "결제 만료",
                )
            "WAITING_FOR_DEPOSIT", "IN_PROGRESS", "READY" ->
                logSystem(payment, orderId, paymentKey, remote.rawBody, "토스 상태 보류: $status")
            "PARTIAL_CANCELED" ->
                logSystem(payment, orderId, paymentKey, remote.rawBody, "부분 취소 웹훅은 별도 처리 필요")
            else -> logSystem(payment, orderId, paymentKey, remote.rawBody, "토스 상태 미처리: $status")
        }
    }

    private fun flagLateApproval(
        payment: Payment,
        json: JsonNode,
        remote: TossApiResponse,
    ) {
        if (!TossPaymentResponseMapper.isDoneWithMatchingAmount(json, payment.amount)) {
            logSystem(payment, payment.orderId, payment.paymentKey, remote.rawBody, "종료된 결제 상태: FAILED")
            return
        }
        val alreadyFlagged =
            paymentLogJpaRepository.existsByPaymentIdAndErrorMessage(
                payment.id,
                TossWebhookService.LATE_APPROVAL_MESSAGE,
            )
        if (alreadyFlagged) {
            return
        }
        log.error("{} paymentId={}", TossWebhookService.LATE_APPROVAL_MESSAGE, payment.id)
        paymentLogCommandService.append(
            payment = payment,
            orderId = payment.orderId,
            paymentKey = payment.paymentKey,
            logType = PaymentLogType.CONFIRM_ERROR,
            step = PaymentLogStep.SERVER,
            httpStatus = remote.httpStatus,
            responseBody = remote.rawBody,
            errorMessage = TossWebhookService.LATE_APPROVAL_MESSAGE,
        )
    }

    private fun finishRefundingFromRemote(
        payment: Payment,
        json: JsonNode,
        remote: TossApiResponse,
    ) {
        val refundId = paymentRefundTxService.findPendingTossRefund(payment.id)?.refundId
        if (refundId != null && TossPaymentResponseMapper.isFullyCanceled(json, payment.amount)) {
            paymentRefundTxService.completeRefund(refundId, remote)
            return
        }
        logSystem(payment, payment.orderId, payment.paymentKey, remote.rawBody, "환불 처리 중")
    }

    private fun applyDoneFromRemote(
        payment: Payment,
        json: JsonNode,
        rawJson: String?,
        orderId: String,
        paymentKey: String,
    ) {
        if (!TossPaymentResponseMapper.isDoneWithMatchingAmount(json, payment.amount)) {
            logSystem(payment, orderId, paymentKey, rawJson, "결제 금액 불일치")
            return
        }
        finalizeFromRemote(payment, json, rawJson!!)
    }

    private fun finalizeFromRemote(
        payment: Payment,
        json: JsonNode,
        rawJson: String,
    ) {
        TossPaymentResponseMapper.applyConfirmSuccess(payment, json, rawJson)
        val approvedAt = payment.approvedAt ?: LocalDateTime.now()
        payment.markSucceeded(approvedAt)
        reservationPaymentService.finalizeAfterGatewaySuccess(payment)
    }

    private fun logSystem(
        payment: Payment?,
        orderId: String?,
        paymentKey: String?,
        body: String?,
        message: String,
    ) {
        paymentLogCommandService.append(
            payment = payment,
            orderId = orderId,
            paymentKey = paymentKey,
            logType = PaymentLogType.SYSTEM,
            step = PaymentLogStep.SERVER,
            requestBody = body,
            errorMessage = message,
        )
    }
}
