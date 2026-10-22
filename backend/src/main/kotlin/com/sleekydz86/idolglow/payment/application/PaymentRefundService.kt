package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentProvider
import com.sleekydz86.idolglow.payment.domain.PaymentRefund
import com.sleekydz86.idolglow.payment.domain.PaymentRefundStatus
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.domain.RefundRequestedBy
import com.sleekydz86.idolglow.payment.infrastructure.PaymentRefundJpaRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossApiResponse
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class PaymentRefundService(
    private val paymentRefundTxService: PaymentRefundTxService,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val paymentRepository: PaymentRepository,
    private val paymentRefundJpaRepository: PaymentRefundJpaRepository,
    private val tossPaymentProperties: TossPaymentProperties,
    private val paymentNotificationMailService: PaymentNotificationMailService,
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

    @Transactional
    fun adminRetryLastFailedRefund(paymentId: Long): PaymentRefund {
        val payment =
            paymentRepository.findByIdForUpdate(paymentId)
                ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        require(payment.status == PaymentStatus.SUCCEEDED) {
            "환불 재시도는 결제가 아직 승인된 상태에서만 가능합니다."
        }
        val failed = latestFailedRefund(paymentId)
        require(payment.provider == PaymentProvider.TOSS) { "토스 결제만 PG 재시도할 수 있습니다." }
        require(tossPaymentProperties.enabled) { "토스 결제가 비활성화되어 있습니다." }
        val key = payment.paymentKey ?: throw IllegalStateException("토스 paymentKey가 없습니다.")

        val idempotencyKey = "refund-retry-${payment.id}-${UUID.randomUUID()}"
        val retry =
            paymentRefundJpaRepository.save(
                PaymentRefund(
                    payment = payment,
                    reservation = failed.reservation,
                    cancelAmount = failed.cancelAmount,
                    cancelReason = failed.cancelReason + " (재시도)",
                    status = PaymentRefundStatus.PENDING,
                    requestedBy = RefundRequestedBy.ADMIN,
                    idempotencyKey = idempotencyKey,
                ),
            )

        val response =
            tossPaymentsApiClient.cancel(
                paymentKey = key,
                cancelReason = retry.cancelReason,
                cancelAmount = null,
                idempotencyKey = idempotencyKey,
            )

        paymentLogCommandService.append(
            payment = payment,
            orderId = payment.orderId,
            paymentKey = key,
            logType = if (response.isSuccess2xx) PaymentLogType.CANCEL_RESPONSE else PaymentLogType.CANCEL_ERROR,
            step = PaymentLogStep.TOSS_API,
            httpStatus = response.httpStatus,
            responseBody = response.rawBody,
        )

        if (!response.isSuccess2xx || response.json == null) {
            failRetry(retry, response)
        }

        retry.status = PaymentRefundStatus.SUCCEEDED
        retry.rawResponseJson = response.rawBody
        payment.markFullRefund()
        paymentNotificationMailService.sendCanceled(payment)
        return retry
    }

    private fun latestFailedRefund(paymentId: Long): PaymentRefund =
        paymentRefundJpaRepository
            .findAllByPaymentIdOrderByCreatedAtDesc(paymentId)
            .firstOrNull { it.status == PaymentRefundStatus.FAILED }
            ?: throw IllegalStateException("재시도할 실패 환불 이력이 없습니다.")

    private fun failRetry(
        retry: PaymentRefund,
        response: TossApiResponse,
    ): Nothing {
        retry.status = PaymentRefundStatus.FAILED
        retry.failCode = response.json?.path("code")?.asText()
        retry.failMessage = response.json?.path("message")?.asText() ?: response.rawBody
        throw IllegalStateException("재시도 실패: ${retry.failMessage}")
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
