package com.sleekydz86.idolglow.payment.ui

import com.sleekydz86.idolglow.payment.application.PaymentRefundService
import com.sleekydz86.idolglow.payment.application.dto.PaymentRefundResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Admin payment refunds", description = "환불 이력 조회·재시도")
@RestController
@RequestMapping("/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
class AdminPaymentRefundController(
    private val paymentRefundService: PaymentRefundService,
) {

    @Operation(summary = "결제별 환불 이력", description = "최신순")
    @GetMapping("/{paymentId}/refunds")
    fun listRefunds(@PathVariable paymentId: Long): ResponseEntity<List<PaymentRefundResponse>> =
        ResponseEntity.ok(
            paymentRefundService.findRefundsByPaymentId(paymentId).map(PaymentRefundResponse::from)
        )

    @Operation(summary = "마지막 실패 환불 재시도", description = "토스 결제만 PG 재호출")
    @PostMapping("/{paymentId}/refunds/retry")
    fun retry(@PathVariable paymentId: Long): ResponseEntity<PaymentRefundResponse> {
        val refund = paymentRefundService.adminRetryLastFailedRefund(paymentId)
        return ResponseEntity.ok(PaymentRefundResponse.from(refund))
    }
}
