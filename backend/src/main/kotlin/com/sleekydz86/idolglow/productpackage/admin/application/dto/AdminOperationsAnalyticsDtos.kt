package com.sleekydz86.idolglow.productpackage.admin.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate

data class OperationsAnalyticsSummaryResponse(
    @field:Schema(description = "방문일 기준 구간 시작")
    val visitDateFrom: LocalDate,
    @field:Schema(description = "방문일 기준 구간 끝")
    val visitDateTo: LocalDate,
    @field:Schema(description = "해당 방문일 구간에서 상태가 취소인 예약 수")
    val reservationCanceled: Long,
    @field:Schema(description = "해당 방문일 구간에서 상태가 예약 확정인 예약 수")
    val reservationBooked: Long,
    @field:Schema(description = "취소 수 대비 확정 수 기준 취소 비율 취소 확정 취소")
    val cancelRate: BigDecimal?,
    @field:Schema(description = "결제 상태별 건수 키는 PaymentStatus 이름")
    val paymentStatusCounts: Map<String, Long>,
)

data class CancellationComparisonResponse(
    val current: PeriodCancellationMetrics,
    val previous: PeriodCancellationMetrics,
    @field:Schema(description = "이전 구간 대비 현재 구간 취소율 차이")
    val cancelRateDelta: BigDecimal?,
)

data class PeriodCancellationMetrics(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val canceled: Long,
    val booked: Long,
    val cancelRate: BigDecimal?,
)

data class CancelReasonStatRow(
    @field:Schema(description = "취소 사유 코드 널이면 미기록")
    val reason: String?,
    val count: Long,
)

data class ProductConversionRow(
    val productId: Long,
    val productName: String,
    val booked: Long,
    val canceled: Long,
    @field:Schema(description = "확정 대비 확정 취소 합에서 확정이 차지하는 비율")
    val conversionRate: BigDecimal?,
)

data class SlotHourOccupancyRow(
    @field:Schema(description = "시작 시각 시 0 23")
    val hourOfDay: Int,
    val totalSlots: Long,
    val bookedSlots: Long,
    val occupancyRate: BigDecimal?,
)

data class PaymentFailureHourRow(
    val hourOfDay: Int,
    val failureCount: Long,
)

fun computeRate(numerator: Long, denominator: Long): BigDecimal? {
    if (denominator <= 0L) return null
    return BigDecimal.valueOf(numerator)
        .divide(BigDecimal.valueOf(denominator), 4, RoundingMode.HALF_UP)
}
