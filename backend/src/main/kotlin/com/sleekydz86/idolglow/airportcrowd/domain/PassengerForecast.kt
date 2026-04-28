package com.sleekydz86.idolglow.airportcrowd.domain

import java.time.LocalDate

data class PassengerForecast(
    val date: LocalDate?,
    val timeSlot: String,
    val terminal1DepartureTotal: Int?,
    val terminal2DepartureTotal: Int?,
    val terminal1ArrivalTotal: Int?,
    val terminal2ArrivalTotal: Int?,
)
