package com.sleekydz86.idolglow.payment.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal

@Schema(description = "토스 결제 승인 요청 (클라이언트가 paymentKey 수신 후 호출)")
data class TossConfirmPaymentRequest(
    @field:NotBlank
    @field:Schema(description = "토스 paymentKey", example = "tgen...")
    val paymentKey: String,

    @field:NotBlank
    @field:Schema(description = "주문번호(예약 생성 시 내려준 orderId)", example = "ORD_...")
    val orderId: String,

    @field:NotNull
    @field:Schema(description = "결제 금액(원)", example = "50000")
    val amount: BigDecimal,
)
