package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import org.springframework.stereotype.Service
import java.math.BigDecimal

@Service
class TossPaymentConfirmService(
    private val tossPaymentConfirmTxService: TossPaymentConfirmTxService,
    private val paymentRepository: PaymentRepository,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val paymentLogCommandService: PaymentLogCommandService,
) {
    fun confirm(
        userId: Long,
        paymentKey: String,
        orderId: String,
        amount: BigDecimal,
        idempotencyKey: String?,
        traceId: String?,
    ): Payment {
        val prepared = tossPaymentConfirmTxService.prepareConfirm(userId, orderId, amount, idempotencyKey)
        if (prepared.skipGateway) {
            return prepared.payment
        }
        return confirmWithGateway(userId, paymentKey, orderId, amount, prepared, traceId)
    }

    private fun confirmWithGateway(
        userId: Long,
        paymentKey: String,
        orderId: String,
        amount: BigDecimal,
        prepared: TossConfirmPrepareResult,
        traceId: String?,
    ): Payment {
        val amountLong = toKrwLong(amount)
        val requestPayload = """{"paymentKey":"$paymentKey","orderId":"$orderId","amount":$amountLong}"""
        paymentLogCommandService.append(
            payment = prepared.payment,
            orderId = orderId,
            paymentKey = paymentKey,
            logType = PaymentLogType.CONFIRM_REQUEST,
            step = PaymentLogStep.SERVER,
            requestUrl = "/v1/payments/confirm",
            httpMethod = "POST",
            requestBody = requestPayload,
            traceId = traceId,
        )
        val response =
            tossPaymentsApiClient.confirm(
                paymentKey = paymentKey,
                orderId = orderId,
                amount = amountLong,
                idempotencyKey = prepared.idempotencyKey,
            )
        paymentLogCommandService.append(
            payment = prepared.payment,
            orderId = orderId,
            paymentKey = paymentKey,
            logType = if (response.isSuccess2xx) PaymentLogType.CONFIRM_RESPONSE else PaymentLogType.CONFIRM_ERROR,
            step = PaymentLogStep.TOSS_API,
            requestUrl = "/v1/payments/confirm",
            httpMethod = "POST",
            httpStatus = response.httpStatus,
            requestBody = requestPayload,
            responseBody = response.rawBody,
            errorMessage = response.error?.message,
            traceId = traceId,
        )
        if (response.isInconclusive) {
            return paymentRepository.findByOrderId(orderId) ?: prepared.payment
        }
        return tossPaymentConfirmTxService.completeConfirm(userId, orderId, amount, response)
    }

    private fun toKrwLong(amount: BigDecimal): Long {
        val normalized = amount.stripTrailingZeros()
        require(normalized.scale() <= 0) { "원화 결제 금액은 정수여야 합니다." }
        return normalized.longValueExact()
    }
}
