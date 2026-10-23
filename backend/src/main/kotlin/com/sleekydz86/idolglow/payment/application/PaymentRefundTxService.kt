package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentProvider
import com.sleekydz86.idolglow.payment.domain.PaymentRefund
import com.sleekydz86.idolglow.payment.domain.PaymentRefundStatus
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.domain.RefundRequestedBy
import com.sleekydz86.idolglow.payment.infrastructure.PaymentJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.PaymentRefundJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.time.LocalDateTime

@Transactional
@Service
class PaymentRefundTxService(
    private val paymentJpaRepository: PaymentJpaRepository,
    private val paymentRefundJpaRepository: PaymentRefundJpaRepository,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val paymentNotificationMailService: PaymentNotificationMailService,
    private val tossPaymentProperties: TossPaymentProperties,
    private val objectMapper: ObjectMapper,
    private val reservationPaymentLock: ReservationPaymentLock,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun prepareRefund(
        paymentId: Long,
        cancelReason: String,
        requestedBy: RefundRequestedBy,
    ): RefundPrepareResult {
        val payment = reservationPaymentLock.lockByPaymentId(paymentId)
        return when (payment.status) {
            PaymentStatus.REFUNDED -> RefundPrepareResult(payment.id)
            PaymentStatus.REFUNDING -> pendingRefund(payment)
            PaymentStatus.SUCCEEDED -> openRefund(payment, cancelReason, requestedBy)
            else -> throw IllegalArgumentException("승인된 결제만 환불할 수 있습니다. status=${payment.status}")
        }
    }

    fun completeRefund(
        refundId: Long,
        response: TossApiResponse,
    ) {
        val payment = reservationPaymentLock.lockByRefundId(refundId)
        val refund =
            paymentRefundJpaRepository.findById(refundId).orElseThrow {
                IllegalArgumentException("환불 이력을 찾을 수 없습니다: $refundId")
            }
        if (refund.status != PaymentRefundStatus.PENDING || response.isInconclusive) {
            return
        }
        val json = response.json
        if (!response.isSuccess2xx || json == null) {
            markRefundRejected(payment, refund, response)
        } else if (TossPaymentResponseMapper.isFullyCanceled(json, payment.amount)) {
            markRefundSucceeded(payment, refund, response)
        } else {
            val remoteStatus = json.path("status").asText("")
            val balance = json.path("balanceAmount")
            val balanceText =
                if (balance.isNumber) {
                    balance.decimalValue().stripTrailingZeros().toPlainString()
                } else {
                    balance.asText("")
                }
            val detail =
                if (remoteStatus == "PARTIAL_CANCELED") {
                    "부분 취소, status=$remoteStatus, balanceAmount=$balanceText"
                } else {
                    "status=$remoteStatus, balanceAmount=$balanceText"
                }
            val message = "환불 응답이 전액 취소가 아님: $detail"
            log.error("{} paymentId={} refundId={}", message, payment.id, refund.id)
            paymentLogCommandService.append(
                payment = payment,
                orderId = payment.orderId,
                paymentKey = payment.paymentKey,
                logType = PaymentLogType.CANCEL_ERROR,
                step = PaymentLogStep.SERVER,
                httpStatus = response.httpStatus,
                responseBody = response.rawBody,
                errorMessage = message,
            )
        }
    }

    fun prepareAdminRetry(paymentId: Long): RefundPrepareResult {
        val payment = reservationPaymentLock.lockByPaymentId(paymentId)
        require(payment.provider == PaymentProvider.TOSS) { "토스 결제만 PG 재시도할 수 있습니다." }
        require(tossPaymentProperties.enabled) { "토스 결제가 비활성화되어 있습니다." }
        require(!payment.paymentKey.isNullOrBlank()) { "토스 paymentKey가 없습니다." }
        require(payment.status == PaymentStatus.REFUNDING || payment.status == PaymentStatus.SUCCEEDED) {
            "환불 재시도는 결제가 환불 처리 중이거나 승인된 상태에서만 가능합니다."
        }
        val refunds = paymentRefundJpaRepository.findAllByPaymentIdOrderByCreatedAtDesc(payment.id)
        check(refunds.none { it.status == PaymentRefundStatus.PENDING }) {
            "이미 환불 결과를 기다리는 중입니다."
        }
        val failed =
            refunds.firstOrNull()?.takeIf { it.status == PaymentRefundStatus.FAILED }
                ?: throw IllegalStateException("재시도할 실패 환불 이력이 없습니다.")
        if (payment.status == PaymentStatus.SUCCEEDED) {
            payment.markRefunding()
        }
        savePending(
            payment,
            failed.cancelReason + " (재시도)",
            RefundRequestedBy.ADMIN,
            failed.reservation,
        )
        return pendingRefund(payment)
    }

    @Transactional(readOnly = true)
    fun findPendingTossRefund(paymentId: Long): RefundPrepareResult? {
        val payment = paymentJpaRepository.findById(paymentId).orElse(null)
        if (payment == null || payment.status != PaymentStatus.REFUNDING || payment.provider != PaymentProvider.TOSS) {
            return null
        }
        return pendingRefund(payment).takeIf { it.needsTossCancel }
    }

    @Transactional(readOnly = true)
    fun findPaymentIdsToReconcile(
        requestedBefore: LocalDateTime,
        stopRetryBefore: LocalDateTime,
        stopMessage: String,
        stopLoggedSince: LocalDateTime,
        limit: Int,
    ): List<Long> =
        paymentRefundJpaRepository
            .findIdsToReconcile(
                requestedBefore,
                stopRetryBefore,
                stopMessage,
                stopLoggedSince,
                PageRequest.of(0, limit),
            ).distinct()

    @Transactional(readOnly = true)
    fun loadPayment(paymentId: Long): Payment =
        paymentJpaRepository.findById(paymentId).orElseThrow {
            IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        }

    private fun openRefund(
        payment: Payment,
        cancelReason: String,
        requestedBy: RefundRequestedBy,
    ): RefundPrepareResult {
        if (payment.provider == PaymentProvider.TOSS) {
            require(tossPaymentProperties.enabled) { "토스 결제가 비활성화되어 있습니다." }
            require(!payment.paymentKey.isNullOrBlank()) { "토스 paymentKey 가 없어 환불할 수 없습니다." }
        }
        payment.markRefunding()
        val refund = savePending(payment, cancelReason, requestedBy)
        if (payment.provider == PaymentProvider.MOCK) {
            payment.markFullRefund()
            refund.status = PaymentRefundStatus.SUCCEEDED
            refund.rawResponseJson = objectMapper.createObjectNode().put("mock", true).toString()
            paymentLogCommandService.append(
                payment = payment,
                orderId = payment.orderId,
                paymentKey = payment.paymentKey,
                logType = PaymentLogType.CANCEL_RESPONSE,
                step = PaymentLogStep.SERVER,
                httpStatus = 200,
                responseBody = refund.rawResponseJson,
            )
            paymentNotificationMailService.sendCanceled(payment)
            return RefundPrepareResult(paymentId = payment.id, refundId = refund.id)
        }
        return RefundPrepareResult(
            paymentId = payment.id,
            orderId = payment.orderId,
            refundId = refund.id,
            paymentKey = payment.paymentKey,
            idempotencyKey = refund.idempotencyKey,
            cancelReason = refund.cancelReason,
            needsTossCancel = true,
        )
    }

    private fun markRefundRejected(
        payment: Payment,
        refund: PaymentRefund,
        response: TossApiResponse,
    ) {
        refund.status = PaymentRefundStatus.FAILED
        refund.failCode = response.json?.path("code")?.asText()
        refund.failMessage = response.json?.path("message")?.asText() ?: response.rawBody
        log.error(
            "토스 환불이 거절되었습니다. paymentId={} refundId={} httpStatus={} code={}",
            payment.id,
            refund.id,
            response.httpStatus,
            refund.failCode,
        )
    }

    private fun markRefundSucceeded(
        payment: Payment,
        refund: PaymentRefund,
        response: TossApiResponse,
    ) {
        refund.status = PaymentRefundStatus.SUCCEEDED
        refund.rawResponseJson = response.rawBody
        refund.externalTransactionKey = TossPaymentResponseMapper.cancelTransactionKey(response.json!!)
        payment.markFullRefund()
        paymentNotificationMailService.sendCanceled(payment)
    }

    private fun savePending(
        payment: Payment,
        cancelReason: String,
        requestedBy: RefundRequestedBy,
        reservation: Reservation = payment.reservation,
    ): PaymentRefund {
        val refund =
            paymentRefundJpaRepository.saveAndFlush(
                PaymentRefund(
                    payment = payment,
                    reservation = reservation,
                    cancelAmount = payment.amount,
                    cancelReason = cancelReason,
                    status = PaymentRefundStatus.PENDING,
                    requestedBy = requestedBy,
                    firstRequestedAt = LocalDateTime.now(),
                ),
            )
        refund.idempotencyKey = "refund-${refund.id}"
        return refund
    }

    private fun pendingRefund(payment: Payment): RefundPrepareResult {
        val pending =
            paymentRefundJpaRepository
                .findAllByPaymentIdOrderByCreatedAtDesc(payment.id)
                .firstOrNull { it.status == PaymentRefundStatus.PENDING }
        if (pending == null || payment.provider != PaymentProvider.TOSS) {
            return RefundPrepareResult(payment.id)
        }
        return RefundPrepareResult(
            paymentId = payment.id,
            orderId = payment.orderId,
            refundId = pending.id,
            paymentKey = payment.paymentKey,
            idempotencyKey = pending.idempotencyKey,
            cancelReason = pending.cancelReason,
            needsTossCancel = true,
            firstRequestedAt = pending.firstRequestedAt,
        )
    }
}
