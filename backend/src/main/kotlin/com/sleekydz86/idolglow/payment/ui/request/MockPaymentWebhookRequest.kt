package com.sleekydz86.idolglow.payment.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

data class MockPaymentWebhookRequest(
    @field:Schema(description = "Payment reference", example = "pay_mock_20260321120000_1")
    @field:NotBlank
    val paymentReference: String,
    @field:Schema(description = "Webhook result", example = "SUCCEEDED")
    val status: MockPaymentWebhookStatus,
    @field:Schema(description = "Failure reason", example = "issuer declined")
    val failureReason: String? = null,
)
