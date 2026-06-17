package com.sleekydz86.idolglow.productpackage.admin.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
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
