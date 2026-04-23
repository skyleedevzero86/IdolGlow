package com.sleekydz86.idolglow.payment.ui

import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import com.sleekydz86.idolglow.payment.application.MyPagePaymentService
import com.sleekydz86.idolglow.payment.application.dto.MyPagePaymentSummaryResponse
import com.sleekydz86.idolglow.payment.ui.request.CancelPaymentRequest
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

@RestController
@RequestMapping("/my/payments")
class MyPaymentController(
    private val myPagePaymentService: MyPagePaymentService,
) {
    @GetMapping
    fun list(@LoginUser userId: Long): ResponseEntity<List<MyPagePaymentSummaryResponse>> =
        ResponseEntity.ok(myPagePaymentService.findPayments(userId))

    @GetMapping("/{paymentId}")
    fun detail(
        @LoginUser userId: Long,
        @PathVariable paymentId: Long,
    ): ResponseEntity<MyPagePaymentSummaryResponse> =
        ResponseEntity.ok(myPagePaymentService.findPayment(userId, paymentId))

    @PostMapping("/{paymentId}/cancel")
    fun cancel(
        @LoginUser userId: Long,
        @PathVariable paymentId: Long,
        @RequestBody(required = false) request: CancelPaymentRequest?,
    ): ResponseEntity<MyPagePaymentSummaryResponse> =
        ResponseEntity.ok(myPagePaymentService.cancelPayment(userId, paymentId, request?.reason))

    @GetMapping("/{paymentId}/receipt.pdf")
    fun receipt(
        @LoginUser userId: Long,
        @PathVariable paymentId: Long,
    ): ResponseEntity<Resource> {
        val bytes = myPagePaymentService.renderReceiptPdf(userId, paymentId)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=my-payment-$paymentId-receipt.pdf")
            .body(ByteArrayResource(bytes))
    }
}
