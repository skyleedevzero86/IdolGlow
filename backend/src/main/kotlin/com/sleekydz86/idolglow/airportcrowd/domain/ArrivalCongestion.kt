package com.sleekydz86.idolglow.airportcrowd.domain

import java.time.LocalDateTime

data class ArrivalCongestion(
    val terminal: String,
    val airport: String?,
    val entryGate: String?,
    val gateNumber: String?,
    val flightId: String?,
    val korean: Int?,
    val foreigner: Int?,
    val scheduleTime: LocalDateTime?,
    val estimatedTime: LocalDateTime?,
)
