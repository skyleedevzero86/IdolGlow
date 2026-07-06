package com.sleekydz86.idolglow.productpackage.admin.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

data class SlotHourOccupancyRow(
    @field:Schema(description = "시작 시각 시 0 23")
    val hourOfDay: Int,
    val totalSlots: Long,
    val bookedSlots: Long,
    val occupancyRate: BigDecimal?,
)
