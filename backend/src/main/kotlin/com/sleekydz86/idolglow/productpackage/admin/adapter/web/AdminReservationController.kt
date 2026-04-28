package com.sleekydz86.idolglow.productpackage.admin.ui

import com.sleekydz86.idolglow.productpackage.admin.application.AdminReservationService
import com.sleekydz86.idolglow.productpackage.admin.application.dto.AdminReservationSummaryResponse
import com.sleekydz86.idolglow.productpackage.admin.application.dto.ReservationDashboardResponse
import com.sleekydz86.idolglow.productpackage.admin.ui.request.UpdateAdminMarkdownRequest
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@PreAuthorize("hasRole('ADMIN')")
@RestController
@RequestMapping("/admin/reservations")
class AdminReservationController(
    private val adminReservationService: AdminReservationService,
) {

    @GetMapping("/dashboard")
    fun findDashboard(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) fromDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) toDate: LocalDate?,
        @RequestParam(defaultValue = "10") recentSize: Int,
    ): ResponseEntity<ReservationDashboardResponse> =
        ResponseEntity.ok(
            adminReservationService.findDashboard(
                fromDate = fromDate,
                toDate = toDate,
                recentSize = recentSize.coerceIn(1, 50),
            ),
        )

    @GetMapping
    fun findReservations(
        @RequestParam(required = false) status: ReservationStatus?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) visitDate: LocalDate?,
        @RequestParam(required = false) productId: Long?,
        @RequestParam(defaultValue = "50") size: Int,
    ): ResponseEntity<List<AdminReservationSummaryResponse>> =
        ResponseEntity.ok(
            adminReservationService.findReservations(
                status = status,
                visitDate = visitDate,
                productId = productId,
                size = size.coerceIn(1, 100),
            ),
        )

    @PostMapping("/{reservationId}/cancel")
    fun cancelReservation(@PathVariable reservationId: Long): ResponseEntity<AdminReservationSummaryResponse> =
        ResponseEntity.ok(adminReservationService.cancelReservation(reservationId))

    @PatchMapping("/{reservationId}/memo")
    fun updateAdminMemo(
        @PathVariable reservationId: Long,
        @RequestBody request: UpdateAdminMarkdownRequest,
    ): ResponseEntity<AdminReservationSummaryResponse> =
        ResponseEntity.ok(adminReservationService.updateAdminMemo(reservationId, request.markdown))
}
