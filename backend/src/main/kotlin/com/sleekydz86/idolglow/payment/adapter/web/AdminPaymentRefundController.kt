package com.sleekydz86.idolglow.payment.ui

import com.sleekydz86.idolglow.payment.application.AdminPaymentService
import com.sleekydz86.idolglow.payment.application.PaymentRefundService
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentDetailResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentChartsResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentOverviewResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentSummaryResponse
import com.sleekydz86.idolglow.payment.application.dto.PaymentRefundResponse
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.payment.ui.request.CancelPaymentRequest
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
class AdminPaymentRefundController(
    private val adminPaymentService: AdminPaymentService,
    private val paymentRefundService: PaymentRefundService,
) {

    @GetMapping("/overview")
    fun overview(
        @RequestParam(required = false) status: PaymentStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDate: LocalDate?,
        @RequestParam(required = false) productId: Long?,
    ): ResponseEntity<AdminPaymentOverviewResponse> =
        ResponseEntity.ok(
            adminPaymentService.overview(
                status = status,
                visitDate = visitDate,
                productId = productId,
            )
        )

    @GetMapping
    fun listPayments(
        @RequestParam(required = false) status: PaymentStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDate: LocalDate?,
        @RequestParam(required = false) productId: Long?,
        @RequestParam(defaultValue = "50") size: Int,
    ): ResponseEntity<List<AdminPaymentSummaryResponse>> =
        ResponseEntity.ok(
            adminPaymentService.findPayments(
                status = status,
                visitDate = visitDate,
                productId = productId,
                size = size.coerceIn(1, 200),
            )
        )

    @GetMapping("/charts")
    fun charts(
        @RequestParam(required = false) status: PaymentStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDate: LocalDate?,
        @RequestParam(required = false) productId: Long?,
    ): ResponseEntity<AdminPaymentChartsResponse> =
        ResponseEntity.ok(
            adminPaymentService.charts(
                status = status,
                visitDate = visitDate,
                productId = productId,
            )
        )

    @GetMapping("/{paymentId}")
    fun detail(@PathVariable paymentId: Long): ResponseEntity<AdminPaymentDetailResponse> =
        ResponseEntity.ok(adminPaymentService.findPaymentDetail(paymentId))

    @PostMapping("/{paymentId}/cancel")
    fun cancel(
        @PathVariable paymentId: Long,
        @RequestBody(required = false) request: CancelPaymentRequest?,
    ): ResponseEntity<AdminPaymentDetailResponse> {
        return ResponseEntity.ok(adminPaymentService.cancelPayment(paymentId, request?.reason))
    }

    @GetMapping("/{paymentId}/refunds")
    fun listRefunds(@PathVariable paymentId: Long): ResponseEntity<List<PaymentRefundResponse>> =
        ResponseEntity.ok(paymentRefundService.findRefundsByPaymentId(paymentId).map(PaymentRefundResponse::from))

    @PostMapping("/{paymentId}/refunds/retry")
    fun retry(@PathVariable paymentId: Long): ResponseEntity<PaymentRefundResponse> {
        val refund = paymentRefundService.adminRetryLastFailedRefund(paymentId)
        return ResponseEntity.ok(PaymentRefundResponse.from(refund))
    }

    @GetMapping("/export.xlsx")
    fun exportXlsx(
        @RequestParam(required = false) status: PaymentStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDate: LocalDate?,
        @RequestParam(required = false) productId: Long?,
    ): ResponseEntity<Resource> {
        val bytes = adminPaymentService.exportPaymentsXlsx(
            status = status,
            visitDate = visitDate,
            productId = productId,
        )
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payments.xlsx")
            .body(ByteArrayResource(bytes))
    }

    @GetMapping("/{paymentId}/receipt.pdf")
    fun receipt(@PathVariable paymentId: Long): ResponseEntity<Resource> {
        val bytes = adminPaymentService.renderReceiptPdf(paymentId)
        return ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payment-$paymentId-receipt.pdf")
            .body(ByteArrayResource(bytes))
    }
}
