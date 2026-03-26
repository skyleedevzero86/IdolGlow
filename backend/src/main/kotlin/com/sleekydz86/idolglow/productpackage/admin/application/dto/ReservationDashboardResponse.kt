package com.sleekydz86.idolglow.productpackage.admin.application.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ReservationDashboardResponse(
    @field:Schema(description = "Pending reservation count", example = "10")
    val pendingCount: Long,
    @field:Schema(description = "Booked reservation count", example = "20")
    val bookedCount: Long,
    @field:Schema(description = "Completed reservation count", example = "30")
    val completedCount: Long,
    @field:Schema(description = "Canceled reservation count", example = "5")
    val canceledCount: Long,
    @field:Schema(description = "Pending payment count", example = "8")
    val paymentPendingCount: Long,
    @field:Schema(description = "Succeeded payment count", example = "18")
    val paymentSucceededCount: Long,
    @field:Schema(description = "Failed payment count", example = "3")
    val paymentFailedCount: Long,
    @field:Schema(description = "Canceled payment count", example = "2")
    val paymentCanceledCount: Long,
    @field:Schema(description = "Expired payment count", example = "1")
    val paymentExpiredCount: Long,
    @field:Schema(description = "Recent reservations")
    val recentReservations: List<AdminReservationSummaryResponse>,
)
