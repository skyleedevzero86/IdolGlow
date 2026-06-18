package com.sleekydz86.idolglow.airportcrowd.application

import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCrowdLevel

data class ParkingCongestionView(
    val terminal: String?,
    val floor: String,
    val parking: Int?,
    val parkingArea: Int?,
    val available: Int?,
    val occupancyRate: Double?,
    val observedAt: java.time.LocalDateTime?,
    val level: DepartureCrowdLevel,
)
