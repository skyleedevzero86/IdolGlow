package com.sleekydz86.idolglow.glowweather.application.port.out

import java.time.LocalDateTime

data class CurrentObservationSnapshot(
    val observedAt: LocalDateTime?,
    val temperatureC: Double?,
    val humidity: Int?,
    val precipitationTypeCode: Int?,
    val windDirectionDeg: Int?,
    val windSpeedMps: Double?,
)
