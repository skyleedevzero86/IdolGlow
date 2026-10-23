package com.sleekydz86.idolglow.payment.application

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.domain.Payment
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.domain.PaymentRepository
import com.sleekydz86.idolglow.payment.infrastructure.TossPaymentsApiClient
import org.springframework.stereotype.Service
import tools.jackson.databind.ObjectMapper

@Service
class TossWebhookService(
    private val objectMapper: ObjectMapper,
    private val paymentRepository: PaymentRepository,
    private val tossPaymentsApiClient: TossPaymentsApiClient,
    private val tossPaymentProperties: TossPaymentProperties,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val tossWebhookTxService: TossWebhookTxService,
    private val billingKeyDeletionService: BillingKeyDeletionService,
) {
    fun handlePayload(
        rawBody: String,
        transmissionId: String? = null,
    ) {
        val root =
            runCatching { objectMapper.readTree(rawBody) }.getOrElse {
                paymentLogCommandService.append(
                    payment = null,
                    orderId = null,
                    paymentKey = null,
                    logType = PaymentLogType.WEBHOOK_RECEIVED,
                    step = PaymentLogStep.SERVER,
                    httpStatus = 200,
                    requestBody = rawBody,
                    errorMessage = "웹훅 JSON 파싱 실패",
                    traceId = transmissionId,
                )
                return
            }

        val eventType = root.path("eventType").asText(null)
        if (eventType == "BILLING_DELETED") {
            if (tossPaymentProperties.enabled) {
                billingKeyDeletionService.acceptBillingDeleted(root.path("data"))
            }
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
            traceId = transmissionId,
        )

        if (!tossPaymentProperties.enabled) {
            logSystem(null, logOrderId, logPaymentKey, rawBody, "토스 결제 비활성화, 웹훅 처리 생략")
            return
        }

        if (eventType != "PAYMENT_STATUS_CHANGED" || data.isMissingNode || data.isNull) {
            logSystem(null, logOrderId, logPaymentKey, rawBody, "처리 대상 아님: eventType=$eventType")
            return
        }

        val orderId =
            data
                .path("orderId")
                .asText(null)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        val paymentKey =
            data
                .path("paymentKey")
                .asText(null)
                ?.trim()
                ?.takeIf { it.isNotEmpty() }
        if (orderId == null || paymentKey == null) {
            logSystem(null, logOrderId, logPaymentKey, rawBody, "orderId 또는 paymentKey 없음")
            return
        }

        val preview = paymentRepository.findByOrderId(orderId)
        if (tossWebhookTxService.isTerminalOrMissing(preview, orderId, paymentKey, rawBody)) {
            return
        }
        val payment = requireNotNull(preview) { "잠그기 전 조회한 결제가 없습니다." }

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
        tossWebhookTxService.apply(orderId, paymentKey, remote, rawBody)
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

    companion object {
        const val LATE_APPROVAL_MESSAGE = "실패로 정리된 결제가 토스에서 승인됨, 환불 필요, 관리자 확인 필요"
    }
}
