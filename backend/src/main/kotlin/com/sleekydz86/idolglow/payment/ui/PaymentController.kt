package com.sleekydz86.idolglow.payment.ui

import com.sleekydz86.idolglow.payment.application.ReservationPaymentService
import com.sleekydz86.idolglow.payment.application.dto.PaymentResponse
import com.sleekydz86.idolglow.payment.ui.request.MockPaymentWebhookRequest
import com.sleekydz86.idolglow.payment.ui.request.MockPaymentWebhookStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Payment", description = "결제 상태 동기화 및 모의 웹훅 API")
@RestController
@RequestMapping("/payments/mock")
class PaymentController(
    private val reservationPaymentService: ReservationPaymentService,
    @Value("\${payment.mock.webhook-secret}")
    private val webhookSecret: String,
) {

    @Operation(summary = "모의 결제 웹훅 처리", description = "테스트용 결제 상태 변경 웹훅을 받아 예약 결제 상태를 갱신합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "결제 상태 반영 성공",
                content = [Content(schema = Schema(implementation = PaymentResponse::class))]
            ),
            ApiResponse(responseCode = "400", description = "잘못된 요청 또는 웹훅 시크릿 불일치")
        ]
    )
    @PostMapping("/webhook")
    fun handleWebhook(
        @Parameter(description = "모의 웹훅 인증 시크릿", example = "idolglow-mock-secret")
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
