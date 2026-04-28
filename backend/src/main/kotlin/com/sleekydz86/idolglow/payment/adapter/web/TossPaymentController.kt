package com.sleekydz86.idolglow.payment.ui

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.payment.application.TossPaymentConfirmService
import com.sleekydz86.idolglow.payment.application.dto.PaymentResponse
import com.sleekydz86.idolglow.payment.ui.request.TossConfirmPaymentRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "토스 결제", description = "토스페이먼츠 결제 승인")
@RestController
@RequestMapping("/payments/toss")
class TossPaymentController(
    private val tossPaymentConfirmService: TossPaymentConfirmService,
) {

    @Operation(summary = "토스 결제 승인", description = "paymentKey·orderId·amount로 승인 API를 호출하고 DB에 반영합니다. Idempotency-Key 권장.")
    @PostMapping("/confirm")
    fun confirm(
        @LoginUser userId: Long,
        @Valid @RequestBody request: TossConfirmPaymentRequest,
        @RequestHeader(name = "Idempotency-Key", required = false) idempotencyKey: String?,
        @RequestHeader(name = "X-Trace-Id", required = false) traceId: String?,
    ): ResponseEntity<PaymentResponse> {
        val payment = tossPaymentConfirmService.confirm(
            userId = userId,
            paymentKey = request.paymentKey,
            orderId = request.orderId,
            amount = request.amount,
            idempotencyKey = idempotencyKey,
            traceId = traceId,
        )
        return ResponseEntity.ok(PaymentResponse.from(payment))
    }
}
