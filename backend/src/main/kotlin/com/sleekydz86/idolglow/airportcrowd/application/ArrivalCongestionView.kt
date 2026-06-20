package com.sleekydz86.idolglow.airportcrowd.application

import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCrowdLevel

data class ArrivalCongestionView(
    val terminal: String,
    val airport: String?,
    val entryGate: String?,
    val gateNumber: String?,
    val flightId: String?,
    val korean: Int?,
    val foreigner: Int?,
    val totalFlow: Int,
    val scheduleTime: java.time.LocalDateTime?,
    val estimatedTime: java.time.LocalDateTime?,
    val level: DepartureCrowdLevel,
)
