package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.payment.adapter.web.request.CancelPaymentRequest
import com.sleekydz86.idolglow.payment.application.MyPagePaymentService
import com.sleekydz86.idolglow.payment.application.dto.MyPagePaymentSummaryResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "마이페이지 결제", description = "내 결제 조회·취소·영수증 API (/my/payments)")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/my/payments")
class MyPaymentController(
    private val myPagePaymentService: MyPagePaymentService,
) {
    @Operation(summary = "내 결제 목록 조회")
    @GetMapping
    fun list(
        @LoginUser userId: Long,
    ): ResponseEntity<List<MyPagePaymentSummaryResponse>> = ResponseEntity.ok(myPagePaymentService.findPayments(userId))

    @Operation(summary = "내 결제 상세 조회")
    @GetMapping("/{paymentId}")
    fun detail(
        @LoginUser userId: Long,
        @PathVariable paymentId: Long,
    ): ResponseEntity<MyPagePaymentSummaryResponse> = ResponseEntity.ok(myPagePaymentService.findPayment(userId, paymentId))

    @Operation(summary = "내 결제 취소")
    @PostMapping("/{paymentId}/cancel")
    fun cancel(
        @LoginUser userId: Long,
        @PathVariable paymentId: Long,
        @RequestBody(required = false) request: CancelPaymentRequest?,
    ): ResponseEntity<MyPagePaymentSummaryResponse> =
        ResponseEntity.ok(myPagePaymentService.cancelPayment(userId, paymentId, request?.reason))

    @Operation(summary = "내 영수증 PDF 다운로드")
    @GetMapping("/{paymentId}/receipt.pdf")
    fun receipt(
        @LoginUser userId: Long,
        @PathVariable paymentId: Long,
    ): ResponseEntity<Resource> {
        val bytes = myPagePaymentService.renderReceiptPdf(userId, paymentId)
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-payment-$paymentId-receipt.pdf")
            .body(ByteArrayResource(bytes))
    }
}
