package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.config.TossPaymentProperties
import com.sleekydz86.idolglow.payment.application.PaymentLogCommandService
import com.sleekydz86.idolglow.payment.application.TossWebhookService
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.infrastructure.TossWebhookSignatureVerifier
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import tools.jackson.databind.ObjectMapper

@DisplayName("토스 웹훅 수신")
class TossWebhookControllerTest {
    private val signatureVerifier =
        TossWebhookSignatureVerifier(TossPaymentProperties(enabled = true, secretKey = "test-secret"))
    private val paymentLogCommandService = mockk<PaymentLogCommandService>(relaxed = true)
    private val tossWebhookService = mockk<TossWebhookService>(relaxed = true)
    private val mockMvc =
        MockMvcBuilders
            .standaloneSetup(
                TossWebhookController(
                    objectMapper = ObjectMapper(),
                    signatureVerifier = signatureVerifier,
                    paymentLogCommandService = paymentLogCommandService,
                    tossWebhookService = tossWebhookService,
                ),
            ).build()

    @DisplayName("서명 헤더가 없는 결제 상태 웹훅은 접수하고 서비스로 넘긴다")
    @Test
    fun paymentStatusWebhookWithoutSignatureAccepted() {
        val body =
            """{"eventType":"PAYMENT_STATUS_CHANGED","data":{"paymentKey":"pk-1","orderId":"ORD-1","status":"DONE"}}"""

        mockMvc
            .perform(
                post("/payments/toss/webhook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("tosspayments-webhook-transmission-id", "tx-9")
                    .content(body),
            ).andExpect(status().isOk)

        verify(exactly = 1) { tossWebhookService.handlePayload(body, "tx-9") }
    }

    @DisplayName("서명 헤더가 없는 payout.changed는 401이다")
    @Test
    fun payoutWebhookWithoutSignatureRejected() {
        val body = """{"eventType":"payout.changed","data":{"payoutId":"po-1"}}"""

        mockMvc
            .perform(
                post("/payments/toss/webhook")
                    .contentType(MediaType.APPLICATION_JSON)
                    .header("tosspayments-webhook-transmission-id", "tx-payout")
                    .content(body),
            ).andExpect(status().isUnauthorized)

        verify(exactly = 0) { tossWebhookService.handlePayload(any(), any()) }
        verify {
            paymentLogCommandService.append(
                payment = null,
                orderId = null,
                paymentKey = null,
                logType = PaymentLogType.WEBHOOK_REJECTED,
                step = PaymentLogStep.SERVER,
                httpStatus = 401,
                requestBody = body,
                errorMessage = "웹훅 서명 검증 실패",
                traceId = "tx-payout",
            )
        }
    }
}
