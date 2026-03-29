package com.sleekydz86.idolglow.payment.ui

import com.sleekydz86.idolglow.payment.application.PaymentLogCommandService
import com.sleekydz86.idolglow.payment.application.TossWebhookService
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.infrastructure.TossWebhookSignatureVerifier
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/payments/toss")
class TossWebhookController(
    private val signatureVerifier: TossWebhookSignatureVerifier,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val tossWebhookService: TossWebhookService,
) {

    @PostMapping("/webhook", consumes = [org.springframework.http.MediaType.APPLICATION_JSON_VALUE])
    fun handleWebhook(
        @RequestBody rawBody: String,
        @RequestHeader(name = "tosspayments-webhook-signature", required = false) signature: String?,
        @RequestHeader(name = "tosspayments-webhook-transmission-time", required = false) transmissionTime: String?,
    ): ResponseEntity<Void> {
        val ok = signatureVerifier.verify(rawBody, signature, transmissionTime)
        if (!ok) {
            paymentLogCommandService.append(
                payment = null,
                orderId = null,
                paymentKey = null,
                logType = PaymentLogType.WEBHOOK_REJECTED,
                step = PaymentLogStep.SERVER,
                httpStatus = HttpStatus.UNAUTHORIZED.value(),
                requestBody = rawBody,
                errorMessage = "웹훅 서명 검증 실패",
            )
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        tossWebhookService.handleSignedPayload(rawBody)
        return ResponseEntity.ok().build()
    }
}
