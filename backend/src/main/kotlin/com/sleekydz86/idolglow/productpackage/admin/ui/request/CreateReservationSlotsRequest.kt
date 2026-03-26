package com.sleekydz86.idolglow.productpackage.admin.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class CreateReservationSlotsRequest(
    @field:Schema(description = "Slot start date", example = "2026-03-21")
    @field:NotNull
    val startDate: LocalDate,
    @field:Schema(description = "Slot end date", example = "2026-03-25")
    @field:NotNull
    val endDate: LocalDate,
    @field:Schema(description = "First start hour", example = "9")
    @field:Min(9)
    @field:Max(15)
    val startHour: Int = 9,
    @field:Schema(description = "Last end hour", example = "16")
    @field:Min(10)
    @field:Max(16)
    val endHour: Int = 16,
)
