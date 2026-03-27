package com.sleekydz86.idolglow.payment.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank

@Schema(description = "모의 결제 웹훅 요청 DTO")
data class MockPaymentWebhookRequest(
    @field:Schema(description = "결제 참조값", example = "pay_mock_20260321120000_1")
    @field:NotBlank
    val paymentReference: String,
    @field:Schema(description = "웹훅 처리 결과", example = "SUCCEEDED")
    val status: MockPaymentWebhookStatus,
    @field:Schema(description = "결제 실패 사유", example = "issuer declined")
    val failureReason: String? = null,
)
