package com.sleekydz86.idolglow.payment.ui

import com.sleekydz86.idolglow.payment.application.PaymentLogCommandService
import com.sleekydz86.idolglow.payment.application.TossWebhookService
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.infrastructure.TossWebhookSignatureVerifier
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.parameters.RequestBody as OasRequestBody
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirements
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(
    name = "Toss webhook",
    description = "토스페이먼츠 서버가 호출하는 웹훅입니다. 로그인·JWT 없이 서명 헤더로만 검증합니다.",
)
@RestController
@RequestMapping("/payments/toss")
class TossWebhookController(
    private val signatureVerifier: TossWebhookSignatureVerifier,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val tossWebhookService: TossWebhookService,
) {

    @SecurityRequirements
    @Operation(
        summary = "토스 웹훅 수신",
        description = "토스가 PAYMENT_STATUS_CHANGED 등 이벤트를 POST합니다. 본문은 JSON 원문 문자열이며, tosspayments-webhook-signature·tosspayments-webhook-transmission-time 헤더로 서명을 검증합니다. 검증 실패 시 401, 성공 시 200이며 결제·예약 반영은 검증 후 수행됩니다.",
        requestBody = OasRequestBody(
            description = "토스 웹훅 원문 JSON (문자열 그대로 전달)",
            required = true,
            content = [
                Content(
                    mediaType = "application/json",
                    examples = [
                        ExampleObject(
                            name = "PAYMENT_STATUS_CHANGED",
                            value = """{"eventType":"PAYMENT_STATUS_CHANGED","createdAt":"2022-01-01T00:00:00.000000","data":{"paymentKey":"...","orderId":"...","status":"DONE"}}""",
                        ),
                    ],
                ),
            ],
        ),
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "서명 검증 성공, 본문 처리 완료"),
            ApiResponse(responseCode = "401", description = "웹훅 서명 검증 실패"),
        ],
    )
    @PostMapping("/webhook", consumes = [org.springframework.http.MediaType.APPLICATION_JSON_VALUE])
    fun handleWebhook(
        @RequestBody rawBody: String,
        @Parameter(description = "토스 웹훅 HMAC 서명 (문서: tosspayments-webhook-signature)", required = false)
        @RequestHeader(name = "tosspayments-webhook-signature", required = false) signature: String?,
        @Parameter(description = "전송 시각 (문서: tosspayments-webhook-transmission-time)", required = false)
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
