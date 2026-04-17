package com.sleekydz86.idolglow.productpackage.admin.application.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ReservationDashboardResponse(
    @field:Schema(description = "대기 중 예약 건수", example = "10")
    val pendingCount: Long,
    @field:Schema(description = "확정 예약 건수", example = "20")
    val bookedCount: Long,
    @field:Schema(description = "완료 예약 건수", example = "30")
    val completedCount: Long,
    @field:Schema(description = "취소 예약 건수", example = "5")
    val canceledCount: Long,
    @field:Schema(description = "결제 대기 건수", example = "8")
    val paymentPendingCount: Long,
    @field:Schema(description = "결제 성공 건수", example = "18")
    val paymentSucceededCount: Long,
    @field:Schema(description = "결제 실패 건수", example = "3")
    val paymentFailedCount: Long,
    @field:Schema(description = "결제 취소 건수", example = "2")
    val paymentCanceledCount: Long,
    @field:Schema(description = "결제 만료 건수", example = "1")
    val paymentExpiredCount: Long,
    @field:Schema(description = "최근 예약 목록")
    val recentReservations: List<AdminReservationSummaryResponse>,
)
