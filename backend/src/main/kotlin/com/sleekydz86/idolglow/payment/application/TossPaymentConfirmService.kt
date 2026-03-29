package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentProvider
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.LocalDateTime
import java.util.UUID

@Transactional
@Service
class TossPaymentConfirmService(
    private val paymentRepository: PaymentRepository,
    private val reservationPaymentService: ReservationPaymentService,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val tossPaymentProperties: TossPaymentProperties,
) {

    fun confirm(
        userId: Long,
        paymentKey: String,
        orderId: String,
        amount: BigDecimal,
        idempotencyKey: String?,
        traceId: String?,
    ): Payment {
        require(tossPaymentProperties.enabled) { "토스 결제가 비활성화되어 있습니다." }

        val existingByIdem = idempotencyKey?.let { paymentRepository.findByIdempotencyKeyForUpdate(it) }
        if (existingByIdem != null && existingByIdem.status == PaymentStatus.SUCCEEDED) {
            require(existingByIdem.reservation.userId == userId) { "본인 예약 결제만 승인할 수 있습니다." }
            return existingByIdem
        }

        val payment = paymentRepository.findByOrderIdForUpdate(orderId)
            ?: throw IllegalArgumentException("주문을 찾을 수 없습니다: $orderId")

        require(payment.reservation.userId == userId) { "본인 예약 결제만 승인할 수 있습니다." }

        require(payment.provider == PaymentProvider.TOSS) { "토스 결제 건이 아닙니다." }
        require(payment.amount.compareTo(amount) == 0) { "결제 금액이 일치하지 않습니다." }

        if (payment.status == PaymentStatus.SUCCEEDED) {
            return payment
        }
        require(payment.status == PaymentStatus.PENDING) { "처리할 수 없는 결제 상태입니다: ${payment.status}" }

        if (!idempotencyKey.isNullOrBlank()) {
            payment.idempotencyKey = idempotencyKey
        }

        val amountLong = toKrwLong(amount)
        val requestPayload =
            """{"paymentKey":"$paymentKey","orderId":"$orderId","amount":$amountLong}"""

        paymentLogCommandService.append(
            payment = payment,
            orderId = orderId,
            paymentKey = paymentKey,
            logType = PaymentLogType.CONFIRM_REQUEST,
            step = PaymentLogStep.SERVER,
            requestUrl = "/v1/payments/confirm",
            httpMethod = "POST",
            requestBody = requestPayload,
            traceId = traceId,
        )

        val response = tossPaymentsApiClient.confirm(
            paymentKey = paymentKey,
            orderId = orderId,
            amount = amountLong,
            idempotencyKey = idempotencyKey ?: "idem-confirm-${payment.id}-${UUID.randomUUID()}",
        )

        paymentLogCommandService.append(
            payment = payment,
            orderId = orderId,
            paymentKey = paymentKey,
            logType = if (response.isSuccess2xx) PaymentLogType.CONFIRM_RESPONSE else PaymentLogType.CONFIRM_ERROR,
            step = PaymentLogStep.TOSS_API,
            requestUrl = "/v1/payments/confirm",
            httpMethod = "POST",
            httpStatus = response.httpStatus,
            responseBody = response.rawBody,
            errorMessage = response.error?.message,
            traceId = traceId,
        )

        if (!response.isSuccess2xx || response.json == null) {
            payment.markFailed(
                reason = response.json?.path("message")?.asText() ?: response.rawBody ?: "토스 승인 실패",
                code = response.json?.path("code")?.asText(),
            )
            payment.rawResponseJson = response.rawBody
            return payment
        }

        TossPaymentResponseMapper.applyConfirmSuccess(payment, response.json, response.rawBody!!)
        val approvedAt = payment.approvedAt ?: LocalDateTime.now()
        payment.markSucceeded(approvedAt)

        return reservationPaymentService.finalizeAfterGatewaySuccess(payment)
    }

    private fun toKrwLong(amount: BigDecimal): Long {
        val normalized = amount.stripTrailingZeros()
        require(normalized.scale() <= 0) { "원화 결제 금액은 정수여야 합니다." }
        return normalized.longValueExact()
    }
}
