package com.sleekydz86.idolglow.airportcrowd.domain

import java.time.LocalDateTime

data class DepartureCongestion(
    val gateId: String,
    val terminalId: String,
    val waitTimeMinutes: Int?,
    val waitLength: Int?,
    val occurredAt: LocalDateTime?,
    val operatingTime: String?,
)

enum class DepartureCrowdLevel {
    SMOOTH,
    MODERATE,
    BUSY,
    HEAVY,
    UNKNOWN,
}
