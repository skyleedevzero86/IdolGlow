package com.sleekydz86.idolglow.payment.application

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
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

@Transactional
@Service
class TossWebhookService(
    private val objectMapper: ObjectMapper,
    private val paymentRepository: PaymentRepository,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val tossPaymentProperties: TossPaymentProperties,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val reservationPaymentService: ReservationPaymentService,
) {

    fun handleSignedPayload(rawBody: String) {
        val root = runCatching { objectMapper.readTree(rawBody) }.getOrElse {
            paymentLogCommandService.append(
                payment = null,
                orderId = null,
                paymentKey = null,
                logType = PaymentLogType.WEBHOOK_RECEIVED,
                step = PaymentLogStep.SERVER,
                httpStatus = 200,
                requestBody = rawBody,
                errorMessage = "웹훅 JSON 파싱 실패",
            )
            return
        }

        val data = root.path("data")
        val logOrderId =
            if (!data.isMissingNode && !data.isNull) data.path("orderId").asText(null) else null
        val logPaymentKey =
            if (!data.isMissingNode && !data.isNull) data.path("paymentKey").asText(null) else null

        paymentLogCommandService.append(
            payment = null,
            orderId = logOrderId,
            paymentKey = logPaymentKey,
            logType = PaymentLogType.WEBHOOK_RECEIVED,
            step = PaymentLogStep.SERVER,
            httpStatus = 200,
            requestBody = rawBody,
        )

        if (!tossPaymentProperties.enabled) {
            logSystem(null, logOrderId, logPaymentKey, rawBody, "토스 결제 비활성화, 웹훅 처리 생략")
            return
        }

        val eventType = root.path("eventType").asText(null)
        if (eventType != "PAYMENT_STATUS_CHANGED" || data.isMissingNode || data.isNull) {
            logSystem(null, logOrderId, logPaymentKey, rawBody, "처리 대상 아님: eventType=$eventType")
            return
        }

        val orderId = data.path("orderId").asText(null)?.trim()?.takeIf { it.isNotEmpty() }
        val paymentKey = data.path("paymentKey").asText(null)?.trim()?.takeIf { it.isNotEmpty() }
        if (orderId == null || paymentKey == null) {
            logSystem(null, logOrderId, logPaymentKey, rawBody, "orderId 또는 paymentKey 없음")
            return
        }

        val payment = paymentRepository.findByOrderIdForUpdate(orderId)
        if (payment == null) {
            logSystem(null, orderId, paymentKey, rawBody, "orderId에 해당하는 결제 없음")
            return
        }

        if (payment.provider != PaymentProvider.TOSS) {
            logSystem(payment, orderId, paymentKey, rawBody, "TOSS 결제가 아님")
            return
        }

        when (payment.status) {
            PaymentStatus.SUCCEEDED -> {
                logSystem(payment, orderId, paymentKey, rawBody, "이미 승인됨(멱등)")
                return
            }
            PaymentStatus.FAILED, PaymentStatus.CANCELED, PaymentStatus.EXPIRED,
            PaymentStatus.REFUNDED, PaymentStatus.PARTIAL_CANCELED,
            -> {
                logSystem(payment, orderId, paymentKey, rawBody, "종료된 결제 상태: ${payment.status}")
                return
            }
            PaymentStatus.PENDING -> Unit
        }

        val remote = tossPaymentsApiClient.getPayment(paymentKey)
        paymentLogCommandService.append(
            payment = payment,
            orderId = orderId,
            paymentKey = paymentKey,
            logType = PaymentLogType.SYSTEM,
            step = PaymentLogStep.TOSS_API,
            requestUrl = "/v1/payments/{paymentKey}",
            httpMethod = "GET",
            httpStatus = remote.httpStatus,
            responseBody = remote.rawBody,
            errorMessage = remote.error?.message,
        )

        if (!remote.isSuccess2xx || remote.json == null) {
            logSystem(payment, orderId, paymentKey, rawBody, "토스 결제 조회 실패")
            return
        }

        val pjson = remote.json!!
        val remoteOrderId = pjson.path("orderId").asText(null)
        if (remoteOrderId != payment.orderId) {
            logSystem(payment, orderId, paymentKey, remote.rawBody, "orderId 불일치")
            return
        }

        if (!amountMatches(pjson, payment.amount)) {
            logSystem(payment, orderId, paymentKey, remote.rawBody, "결제 금액 불일치")
            return
        }

        val status = pjson.path("status").asText(null)?.trim() ?: ""
        when (status) {
            "DONE" -> finalizeFromRemote(payment, pjson, remote.rawBody!!)
            "CANCELED" -> reservationPaymentService.handlePaymentCanceled(
                payment.paymentReference,
                "결제 취소",
            )
            "ABORTED" -> reservationPaymentService.handlePaymentCanceled(
                payment.paymentReference,
                "결제 중단",
            )
            "EXPIRED" -> reservationPaymentService.handlePaymentFailed(
                payment.paymentReference,
                "결제 만료",
            )
            "WAITING_FOR_DEPOSIT", "IN_PROGRESS", "READY" -> {
                logSystem(payment, orderId, paymentKey, remote.rawBody, "토스 상태 보류: $status")
            }
            "PARTIAL_CANCELED" -> {
                logSystem(payment, orderId, paymentKey, remote.rawBody, "부분 취소 웹훅은 별도 처리 필요")
            }
            else -> {
                logSystem(payment, orderId, paymentKey, remote.rawBody, "토스 상태 미처리: $status")
            }
        }
    }

    private fun finalizeFromRemote(payment: Payment, json: JsonNode, rawJson: String) {
        TossPaymentResponseMapper.applyConfirmSuccess(payment, json, rawJson)
        val approvedAt = payment.approvedAt ?: LocalDateTime.now()
        payment.markSucceeded(approvedAt)
        reservationPaymentService.finalizeAfterGatewaySuccess(payment)
    }

    private fun amountMatches(json: JsonNode, expected: BigDecimal): Boolean {
        val total = json.path("totalAmount")
        if (!total.isNumber) return false
        return BigDecimal.valueOf(total.asLong()).compareTo(expected.stripTrailingZeros()) == 0
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
