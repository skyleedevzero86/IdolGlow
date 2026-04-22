package com.sleekydz86.idolglow.productpackage.admin.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

@Schema(description = "예약 슬롯 일괄 생성 요청 DTO")
data class CreateReservationSlotsRequest(
    @field:Schema(description = "슬롯 생성 시작일", example = "2026-03-21")
    @field:NotNull
    val startDate: LocalDate,
    @field:Schema(description = "슬롯 생성 종료일", example = "2026-03-25")
    @field:NotNull
    val endDate: LocalDate,
    @field:Schema(description = "첫 예약 시작 시각", example = "9")
    @field:Min(9)
    @field:Max(15)
    val startHour: Int = 9,
    @field:Schema(description = "마지막 예약 종료 시각", example = "16")
    @field:Min(10)
    @field:Max(16)
    val endHour: Int = 16,

    @field:Schema(description = "토요일 일요일 제외 여부")
    val excludeWeekends: Boolean = false,
    val adminNote: String? = null,
)
