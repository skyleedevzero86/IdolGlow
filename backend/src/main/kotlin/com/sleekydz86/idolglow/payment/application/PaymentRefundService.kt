package com.sleekydz86.idolglow.payment.application

import tools.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.global.infrastructure.config.TossPaymentProperties
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
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Transactional
@Service
class PaymentRefundService(
    private val paymentRepository: PaymentRepository,
    private val paymentRefundJpaRepository: PaymentRefundJpaRepository,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val tossPaymentProperties: TossPaymentProperties,
    private val objectMapper: ObjectMapper,
    private val paymentNotificationMailService: PaymentNotificationMailService,
) {

    fun refundBeforeReservationCancel(
        payment: Payment,
        reservation: Reservation,
        cancelReason: String,
        requestedBy: RefundRequestedBy,
    ) {
        require(reservation.status == ReservationStatus.BOOKED) { "확정된 예약만 환불 대상입니다." }
        require(payment.status == PaymentStatus.SUCCEEDED) { "승인된 결제만 환불할 수 있습니다." }

        val idempotencyKey = "refund-${payment.id}-${UUID.randomUUID()}"
        val refund = paymentRefundJpaRepository.save(
            PaymentRefund(
                payment = payment,
                reservation = reservation,
                cancelAmount = payment.amount,
                cancelReason = cancelReason,
                status = PaymentRefundStatus.PENDING,
                requestedBy = requestedBy,
                idempotencyKey = idempotencyKey,
            )
        )

        when (payment.provider) {
            PaymentProvider.MOCK -> {
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
            }

            PaymentProvider.TOSS -> {
                require(tossPaymentProperties.enabled) { "토스 결제가 비활성화되어 있습니다." }
                val key = payment.paymentKey
                    ?: throw IllegalStateException("토스 paymentKey 가 없어 환불할 수 없습니다.")

                paymentLogCommandService.append(
                    payment = payment,
                    orderId = payment.orderId,
                    paymentKey = key,
                    logType = PaymentLogType.CANCEL_REQUEST,
                    step = PaymentLogStep.SERVER,
                    requestUrl = "/v1/payments/$key/cancel",
                    httpMethod = "POST",
                    requestBody = """{"cancelReason":"${cancelReason.replace("\"", "'")}"}""",
                )

                val response = tossPaymentsApiClient.cancel(
                    paymentKey = key,
                    cancelReason = cancelReason,
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
                    errorMessage = response.error?.message,
                )

                if (!response.isSuccess2xx || response.json == null) {
                    refund.status = PaymentRefundStatus.FAILED
                    refund.failCode = response.json?.path("code")?.asText()
                    refund.failMessage = response.json?.path("message")?.asText() ?: response.rawBody
                    throw IllegalStateException("토스 환불에 실패했습니다: ${refund.failMessage}")
                }

                refund.status = PaymentRefundStatus.SUCCEEDED
                refund.rawResponseJson = response.rawBody
                refund.externalTransactionKey = response.json.path("transactionKey").asText(null)
                payment.markFullRefund()
                paymentNotificationMailService.sendCanceled(payment)
            }
        }
    }

    fun findRefundsByPaymentId(paymentId: Long): List<PaymentRefund> =
        paymentRefundJpaRepository.findAllByPaymentIdOrderByCreatedAtDesc(paymentId)

    fun adminRetryLastFailedRefund(paymentId: Long): PaymentRefund {
        val payment = paymentRepository.findByIdForUpdate(paymentId)
            ?: throw IllegalArgumentException("결제를 찾을 수 없습니다: $paymentId")
        require(payment.status == PaymentStatus.SUCCEEDED) {
            "환불 재시도는 결제가 아직 승인된 상태에서만 가능합니다."
        }
        val failed = paymentRefundJpaRepository.findAllByPaymentIdOrderByCreatedAtDesc(paymentId)
            .firstOrNull { it.status == PaymentRefundStatus.FAILED }
            ?: throw IllegalStateException("재시도할 실패 환불 이력이 없습니다.")

        require(payment.provider == PaymentProvider.TOSS) { "토스 결제만 PG 재시도할 수 있습니다." }
        require(tossPaymentProperties.enabled) { "토스 결제가 비활성화되어 있습니다." }
        val key = payment.paymentKey ?: throw IllegalStateException("토스 paymentKey가 없습니다.")

        val idempotencyKey = "refund-retry-${payment.id}-${UUID.randomUUID()}"
        val retry = paymentRefundJpaRepository.save(
            PaymentRefund(
                payment = payment,
                reservation = failed.reservation,
                cancelAmount = failed.cancelAmount,
                cancelReason = failed.cancelReason + " (재시도)",
                status = PaymentRefundStatus.PENDING,
                requestedBy = RefundRequestedBy.ADMIN,
                idempotencyKey = idempotencyKey,
            )
        )

        val response = tossPaymentsApiClient.cancel(
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
            retry.status = PaymentRefundStatus.FAILED
            retry.failCode = response.json?.path("code")?.asText()
            retry.failMessage = response.json?.path("message")?.asText() ?: response.rawBody
            throw IllegalStateException("재시도 실패: ${retry.failMessage}")
        }

        retry.status = PaymentRefundStatus.SUCCEEDED
        retry.rawResponseJson = response.rawBody
        payment.markFullRefund()
        paymentNotificationMailService.sendCanceled(payment)
        return retry
    }
}
