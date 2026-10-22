package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.payment.application.PaymentLogCommandService
import com.sleekydz86.idolglow.payment.application.TossWebhookService
import com.sleekydz86.idolglow.payment.domain.PaymentLogStep
import com.sleekydz86.idolglow.payment.domain.PaymentLogType
import com.sleekydz86.idolglow.payment.infrastructure.TossWebhookSignatureVerifier
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
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
import tools.jackson.databind.ObjectMapper
import io.swagger.v3.oas.annotations.parameters.RequestBody as OasRequestBody

@Tag(
    name = "Toss webhook",
    description =
        "토스페이먼츠 서버가 호출하는 웹훅입니다. 로그인·JWT가 없습니다. " +
            "payout.changed·seller.changed는 서명으로, 결제 상태 변경은 토스 결제 조회로 검증합니다.",
)
@RestController
@RequestMapping("/payments/toss")
class TossWebhookController(
    private val objectMapper: ObjectMapper,
    private val signatureVerifier: TossWebhookSignatureVerifier,
    private val paymentLogCommandService: PaymentLogCommandService,
    private val tossWebhookService: TossWebhookService,
) {
    @SecurityRequirements
    @Operation(
        summary = "토스 웹훅 수신",
        description =
            "토스가 이벤트를 POST합니다. payout.changed·seller.changed만 " +
                "tosspayments-webhook-signature·tosspayments-webhook-transmission-time으로 서명을 검증하며, 실패하면 401입니다. " +
                "PAYMENT_STATUS_CHANGED 등 그 외 이벤트는 서명 없이 접수하고, 결제 반영은 토스 결제 조회 결과로만 합니다. " +
                "본문이 JSON이 아니거나 eventType이 없으면 400입니다. " +
                "tosspayments-webhook-transmission-id는 결제 로그에 남깁니다.",
        requestBody =
            OasRequestBody(
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
            ApiResponse(responseCode = "200", description = "접수 완료. 결제 반영은 토스 조회 결과 기준"),
            ApiResponse(responseCode = "400", description = "본문이 JSON이 아니거나 eventType이 없음"),
            ApiResponse(responseCode = "401", description = "서명 대상 웹훅의 서명 검증 실패"),
        ],
    )
    @PostMapping("/webhook", consumes = [org.springframework.http.MediaType.APPLICATION_JSON_VALUE])
    fun handleWebhook(
        @RequestBody rawBody: String,
        @Parameter(description = "토스 웹훅 HMAC 서명 (문서: tosspayments-webhook-signature)", required = false)
        @RequestHeader(name = "tosspayments-webhook-signature", required = false) signature: String?,
        @Parameter(description = "전송 시각 (문서: tosspayments-webhook-transmission-time)", required = false)
        @RequestHeader(name = "tosspayments-webhook-transmission-time", required = false) transmissionTime: String?,
        @Parameter(description = "웹훅 전송 ID (문서: tosspayments-webhook-transmission-id)", required = false)
        @RequestHeader(name = "tosspayments-webhook-transmission-id", required = false) transmissionId: String?,
    ): ResponseEntity<Void> {
        val rejected =
            rejection(readEventType(rawBody), rawBody, signature, transmissionTime, transmissionId)
        if (rejected != null) {
            return rejected
        }
        tossWebhookService.handlePayload(rawBody, transmissionId)
        return ResponseEntity.ok().build()
    }

    private fun rejection(
        eventType: String?,
        rawBody: String,
        signature: String?,
        transmissionTime: String?,
        transmissionId: String?,
    ): ResponseEntity<Void>? =
        when {
            eventType == null ->
                reject(
                    rawBody,
                    transmissionId,
                    HttpStatus.BAD_REQUEST,
                    "웹훅 본문이 JSON이 아니거나 eventType이 없습니다.",
                )
            eventType in SIGNATURE_EVENT_TYPES &&
                !signatureVerifier.verify(rawBody, signature, transmissionTime) ->
                reject(rawBody, transmissionId, HttpStatus.UNAUTHORIZED, "웹훅 서명 검증 실패")
            else -> null
        }

    private fun readEventType(rawBody: String): String? =
        runCatching { objectMapper.readTree(rawBody) }
            .getOrNull()
            ?.path("eventType")
            ?.asText(null)
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

    private fun reject(
        rawBody: String,
        transmissionId: String?,
        status: HttpStatus,
        message: String,
    ): ResponseEntity<Void> {
        paymentLogCommandService.append(
            payment = null,
            orderId = null,
            paymentKey = null,
            logType = PaymentLogType.WEBHOOK_REJECTED,
            step = PaymentLogStep.SERVER,
            httpStatus = status.value(),
            requestBody = rawBody,
            errorMessage = message,
            traceId = transmissionId,
        )
        return ResponseEntity.status(status).build()
    }

    private companion object {
        private val SIGNATURE_EVENT_TYPES = setOf("payout.changed", "seller.changed")
    }
}
