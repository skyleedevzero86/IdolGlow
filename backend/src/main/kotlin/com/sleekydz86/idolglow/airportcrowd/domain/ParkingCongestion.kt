package com.sleekydz86.idolglow.airportcrowd.domain

import java.time.LocalDateTime

data class ParkingCongestion(
    val floor: String,
    val terminal: String?,
    val parking: Int?,
    val parkingArea: Int?,
    val observedAt: LocalDateTime?,
)
