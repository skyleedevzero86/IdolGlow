package com.sleekydz86.idolglow.productpackage.admin.ui

import com.sleekydz86.idolglow.productpackage.admin.application.AdminReservationService
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSummaryResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.ReservationDashboardResponse
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.ArraySchema
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@Tag(name = "관리자 예약", description = "관리자 예약 현황 및 제어 API")
@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/reservations")
class AdminReservationController(
    private val adminReservationService: AdminReservationService,
) {

    @Operation(summary = "예약 대시보드 조회", description = "기간별 예약 통계와 최근 예약 현황을 한 번에 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "예약 대시보드 조회 성공",
                content = [Content(schema = Schema(implementation = ReservationDashboardResponse::class))]
            )
        ]
    )
    @GetMapping("/dashboard")
    fun findDashboard(
        @Parameter(description = "조회 시작일", example = "2026-03-01")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate?,
        @Parameter(description = "조회 종료일", example = "2026-03-31")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate?,
        @Parameter(description = "최근 예약 표시 개수", example = "10")
        @RequestParam(defaultValue = "10") recentSize: Int,
    ): ResponseEntity<ReservationDashboardResponse> =
        ResponseEntity.ok(
            adminReservationService.findDashboard(
                fromDate = fromDate,
                toDate = toDate,
                recentSize = recentSize.coerceIn(1, 50)
            )
        )

    @Operation(summary = "예약 목록 조회", description = "상태, 방문일, 상품 조건으로 예약 목록을 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "예약 목록 조회 성공",
                content = [Content(array = ArraySchema(schema = Schema(implementation = AdminReservationSummaryResponse::class)))]
            )
        ]
    )
    @GetMapping
    fun findReservations(
        @Parameter(description = "예약 상태 필터", example = "PENDING")
        @RequestParam(required = false) status: ReservationStatus?,
        @Parameter(description = "방문일 필터", example = "2026-03-27")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDate: LocalDate?,
        @Parameter(description = "상품 ID 필터", example = "1")
        @RequestParam(required = false) productId: Long?,
        @Parameter(description = "조회 개수", example = "50")
        @RequestParam(defaultValue = "50") size: Int,
    ): ResponseEntity<List<AdminReservationSummaryResponse>> =
        ResponseEntity.ok(
            adminReservationService.findReservations(
                status = status,
                visitDate = visitDate,
                productId = productId,
                size = size.coerceIn(1, 100)
            )
        )

    @Operation(summary = "예약 강제 취소", description = "관리자가 특정 예약을 강제로 취소합니다.")
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "예약 취소 성공",
                content = [Content(schema = Schema(implementation = AdminReservationSummaryResponse::class))]
            )
        ]
    )
    @PostMapping("/{reservationId}/cancel")
    fun cancelReservation(
        @Parameter(description = "취소할 예약 ID", example = "100")
        @PathVariable reservationId: Long
    ): ResponseEntity<AdminReservationSummaryResponse> =
        ResponseEntity.ok(adminReservationService.cancelReservation(reservationId))
}
