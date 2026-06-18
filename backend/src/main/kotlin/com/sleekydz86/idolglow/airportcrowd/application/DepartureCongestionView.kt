package com.sleekydz86.idolglow.airportcrowd.application

import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCrowdLevel

data class DepartureCongestionView(
    val gateId: String,
    val terminalId: String,
    val waitTimeMinutes: Int?,
    val waitLength: Int?,
    val occurredAt: java.time.LocalDateTime?,
    val operatingTime: String?,
    val level: DepartureCrowdLevel,
)
