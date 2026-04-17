package com.sleekydz86.idolglow.productpackage.admin.ui

import com.sleekydz86.idolglow.productpackage.admin.application.AdminAuditService
import com.sleekydz86.idolglow.productpackage.admin.application.AdminExportService
import com.sleekydz86.idolglow.productpackage.admin.application.AdminOperationsAnalyticsService
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminAuditLogResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.CancelReasonStatRow
import com.sleekydz86.idolglow.productpackage.admin.application.dto.CancellationComparisonResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.OperationsAnalyticsSummaryResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.PaymentFailureHourRow
import com.sleekydz86.idolglow.productpackage.admin.application.dto.ProductConversionRow
import com.sleekydz86.idolglow.productpackage.admin.application.dto.SlotHourOccupancyRow
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import org.springframework.core.io.ByteArrayResource
import org.springframework.core.io.Resource
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalTime

@Tag(name = "관리자 운영 분석", description = "관리자 운영 분석 감사 로그 CSV")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/operations")
class AdminOperationsController(
    private val adminOperationsAnalyticsService: AdminOperationsAnalyticsService,
    private val adminExportService: AdminExportService,
    private val adminAuditService: AdminAuditService,
) {

    @Operation(summary = "운영 요약", description = "방문일 구간 기준 취소 확정 건수와 결제 상태 건수")
    @GetMapping("/analytics/summary")
    fun analyticsSummary(
        @Parameter(description = "방문일 시작")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateFrom: LocalDate,
        @Parameter(description = "방문일 종료")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateTo: LocalDate,
    ): ResponseEntity<OperationsAnalyticsSummaryResponse> {
        require(!visitDateTo.isBefore(visitDateFrom)) { "visitDateTo는 visitDateFrom 이상이어야 합니다." }
        return ResponseEntity.ok(adminOperationsAnalyticsService.summary(visitDateFrom, visitDateTo))
    }

    @Operation(summary = "취소율 기간 비교", description = "선택한 방문일 구간과 이전 동일 길이 구간의 취소율 비교")
    @GetMapping("/analytics/cancellation-comparison")
    fun cancellationComparison(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateFrom: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateTo: LocalDate,
    ): ResponseEntity<CancellationComparisonResponse> {
        require(!visitDateTo.isBefore(visitDateFrom)) { "visitDateTo는 visitDateFrom 이상이어야 합니다." }
        return ResponseEntity.ok(adminOperationsAnalyticsService.cancellationComparison(visitDateFrom, visitDateTo))
    }

    @Operation(summary = "취소 사유 집계", description = "취소 처리 시각 기준 구간의 취소 사유별 건수")
    @GetMapping("/analytics/cancel-reasons")
    fun cancelReasons(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) canceledAtFrom: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) canceledAtTo: LocalDate,
    ): ResponseEntity<List<CancelReasonStatRow>> {
        require(!canceledAtTo.isBefore(canceledAtFrom)) { "canceledAtTo는 canceledAtFrom 이상이어야 합니다." }
        val from = canceledAtFrom.atStartOfDay()
        val to = canceledAtTo.atTime(LocalTime.of(23, 59, 59, 999_000_000))
        return ResponseEntity.ok(adminOperationsAnalyticsService.cancelReasonStats(from, to))
    }

    @Operation(summary = "상품별 예약 전환", description = "방문일 구간에서 상품별 확정과 취소 건수")
    @GetMapping("/analytics/product-conversion")
    fun productConversion(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateFrom: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateTo: LocalDate,
    ): ResponseEntity<List<ProductConversionRow>> {
        require(!visitDateTo.isBefore(visitDateFrom)) { "visitDateTo는 visitDateFrom 이상이어야 합니다." }
        return ResponseEntity.ok(adminOperationsAnalyticsService.productConversion(visitDateFrom, visitDateTo))
    }

    @Operation(summary = "시간대별 슬롯 점유", description = "방문일 구간의 예약 슬롯 시작 시각별 점유")
    @GetMapping("/analytics/slot-occupancy-by-hour")
    fun slotOccupancyByHour(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateFrom: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateTo: LocalDate,
    ): ResponseEntity<List<SlotHourOccupancyRow>> {
        require(!visitDateTo.isBefore(visitDateFrom)) { "visitDateTo는 visitDateFrom 이상이어야 합니다." }
        return ResponseEntity.ok(adminOperationsAnalyticsService.slotOccupancyByHour(visitDateFrom, visitDateTo))
    }

    @Operation(summary = "결제 실패 시간대", description = "결제 실패 시각이 구간 안인 건을 시각 기준으로 집계")
    @GetMapping("/analytics/payment-failures-by-hour")
    fun paymentFailuresByHour(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) failedAtFrom: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) failedAtTo: LocalDate,
    ): ResponseEntity<List<PaymentFailureHourRow>> {
        require(!failedAtTo.isBefore(failedAtFrom)) { "failedAtTo는 failedAtFrom 이상이어야 합니다." }
        val from = failedAtFrom.atStartOfDay()
        val to = failedAtTo.atTime(LocalTime.of(23, 59, 59, 999_000_000))
        return ResponseEntity.ok(adminOperationsAnalyticsService.paymentFailuresByHour(from, to))
    }

    @Operation(summary = "감사 로그 최근", description = "최근 관리자 액션 로그 최대 200건")
    @GetMapping("/audit-logs")
    fun auditLogs(): ResponseEntity<List<AdminAuditLogResponse>> =
        ResponseEntity.ok(adminAuditService.findRecentLogs())

    @Operation(summary = "예약 CSV", description = "방문일 구간 예약 내보내기 최대 5000행")
    @GetMapping("/export/reservations.csv", produces = ["text/csv"])
    fun exportReservationsCsv(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateFrom: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateTo: LocalDate,
    ): ResponseEntity<Resource> {
        require(!visitDateTo.isBefore(visitDateFrom)) { "visitDateTo는 visitDateFrom 이상이어야 합니다." }
        val bytes = adminExportService.exportReservationsCsv(visitDateFrom, visitDateTo).toByteArray(StandardCharsets.UTF_8)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=reservations-$visitDateFrom-$visitDateTo.csv")
            .body(ByteArrayResource(bytes))
    }

    @Operation(summary = "결제 CSV", description = "방문일 구간 결제 내보내기 최대 5000행")
    @GetMapping("/export/payments.csv", produces = ["text/csv"])
    fun exportPaymentsCsv(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateFrom: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDateTo: LocalDate,
    ): ResponseEntity<Resource> {
        require(!visitDateTo.isBefore(visitDateFrom)) { "visitDateTo는 visitDateFrom 이상이어야 합니다." }
        val bytes = adminExportService.exportPaymentsCsv(visitDateFrom, visitDateTo).toByteArray(StandardCharsets.UTF_8)
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=payments-$visitDateFrom-$visitDateTo.csv")
            .body(ByteArrayResource(bytes))
    }
}
