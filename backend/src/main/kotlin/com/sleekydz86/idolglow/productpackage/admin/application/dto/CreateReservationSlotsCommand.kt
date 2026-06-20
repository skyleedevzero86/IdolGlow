package com.sleekydz86.idolglow.productpackage.admin.application.dto

import java.time.LocalDate

data class CreateReservationSlotsCommand(
    val startDate: LocalDate,
    val endDate: LocalDate,
    val startHour: Int,
    val endHour: Int,
    val excludeWeekends: Boolean,
    val adminNote: String?,
)
