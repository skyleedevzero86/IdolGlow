package com.sleekydz86.idolglow.payment.ui

import com.sleekydz86.idolglow.payment.application.ReservationPaymentService
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/payments/mock")
class PaymentController(
    private val reservationPaymentService: ReservationPaymentService,
    @Value("\${payment.mock.webhook-secret}")
    private val webhookSecret: String,
) {

    @PostMapping("/webhook")
    fun handleWebhook(
        @RequestHeader("X-Mock-Payment-Secret", required = false) secret: String?,
        @Valid @RequestBody request: MockPaymentWebhookRequest
    ): ResponseEntity<PaymentResponse> {
        require(secret == webhookSecret) { "Invalid webhook secret." }

        val payment = when (request.status) {
            MockPaymentWebhookStatus.SUCCEEDED ->
                reservationPaymentService.handlePaymentSucceeded(request.paymentReference)

            MockPaymentWebhookStatus.FAILED ->
                reservationPaymentService.handlePaymentFailed(
                    paymentReference = request.paymentReference,
                    reason = request.failureReason ?: "mock payment failed"
                )

            MockPaymentWebhookStatus.CANCELED ->
                reservationPaymentService.handlePaymentCanceled(
                    paymentReference = request.paymentReference,
                    reason = request.failureReason ?: "mock payment canceled"
                )
        }

        return ResponseEntity.ok(PaymentResponse.from(payment))
    }
}
