package com.sleekydz86.idolglow.payment.adapter.web

import com.sleekydz86.idolglow.payment.adapter.web.request.CancelPaymentRequest
import com.sleekydz86.idolglow.payment.application.AdminPaymentService
import com.sleekydz86.idolglow.payment.application.PaymentRefundService
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentChartsResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentDetailResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentOverviewResponse
import com.sleekydz86.idolglow.payment.application.dto.AdminPaymentSummaryResponse
import com.sleekydz86.idolglow.payment.application.dto.PaymentRefundResponse
import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
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

@Tag(name = "관리자 결제", description = "관리자 결제 조회·취소·환불·영수증 API")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/admin/payments")
@PreAuthorize("hasRole('ADMIN')")
class AdminPaymentRefundController(
    private val adminPaymentService: AdminPaymentService,
    private val paymentRefundService: PaymentRefundService,
) {
    @Operation(summary = "결제 개요 조회")
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
            ),
        )

    @Operation(summary = "결제 목록 조회")
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
            ),
        )

    @Operation(summary = "결제 차트 조회")
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
            ),
        )

    @Operation(summary = "결제 상세 조회")
    @GetMapping("/{paymentId}")
    fun detail(
        @PathVariable paymentId: Long,
    ): ResponseEntity<AdminPaymentDetailResponse> = ResponseEntity.ok(adminPaymentService.findPaymentDetail(paymentId))

    @Operation(summary = "결제 취소")
    @PostMapping("/{paymentId}/cancel")
    fun cancel(
        @PathVariable paymentId: Long,
        @RequestBody(required = false) request: CancelPaymentRequest?,
    ): ResponseEntity<AdminPaymentDetailResponse> = ResponseEntity.ok(adminPaymentService.cancelPayment(paymentId, request?.reason))

    @Operation(summary = "환불 내역 조회")
    @GetMapping("/{paymentId}/refunds")
    fun listRefunds(
        @PathVariable paymentId: Long,
    ): ResponseEntity<List<PaymentRefundResponse>> =
        ResponseEntity.ok(paymentRefundService.findRefundsByPaymentId(paymentId).map(PaymentRefundResponse::from))

    @Operation(summary = "환불 재시도")
    @PostMapping("/{paymentId}/refunds/retry")
    fun retry(
        @PathVariable paymentId: Long,
    ): ResponseEntity<PaymentRefundResponse> {
        val refund = paymentRefundService.adminRetryLastFailedRefund(paymentId)
        return ResponseEntity.ok(PaymentRefundResponse.from(refund))
    }

    @Operation(summary = "결제 목록 엑셀 내보내기")
    @GetMapping("/export.xlsx")
    fun exportXlsx(
        @RequestParam(required = false) status: PaymentStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDate: LocalDate?,
        @RequestParam(required = false) productId: Long?,
    ): ResponseEntity<Resource> {
        val bytes =
            adminPaymentService.exportPaymentsXlsx(
                status = status,
                visitDate = visitDate,
                productId = productId,
            )
        return ResponseEntity
            .ok()
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payments.xlsx")
            .body(ByteArrayResource(bytes))
    }

    @Operation(summary = "영수증 PDF 다운로드")
    @GetMapping("/{paymentId}/receipt.pdf")
    fun receipt(
        @PathVariable paymentId: Long,
    ): ResponseEntity<Resource> {
        val bytes = adminPaymentService.renderReceiptPdf(paymentId)
        return ResponseEntity
            .ok()
            .contentType(MediaType.APPLICATION_PDF)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payment-$paymentId-receipt.pdf")
            .body(ByteArrayResource(bytes))
    }
}
