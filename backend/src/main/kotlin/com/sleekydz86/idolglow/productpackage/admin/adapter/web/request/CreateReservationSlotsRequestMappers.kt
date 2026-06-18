package com.sleekydz86.idolglow.productpackage.admin.adapter.web.request

import com.sleekydz86.idolglow.productpackage.admin.application.dto.CreateReservationSlotsCommand

fun CreateReservationSlotsRequest.toCommand(): CreateReservationSlotsCommand =
    CreateReservationSlotsCommand(
        startDate = startDate,
        endDate = endDate,
        startHour = startHour,
        endHour = endHour,
        excludeWeekends = excludeWeekends,
        adminNote = adminNote,
    )
