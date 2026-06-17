package com.sleekydz86.idolglow.glowweather.application.port.out

import java.time.LocalDate

data class AsosDailySnapshot(
    val date: LocalDate,
    val averageTempC: Double?,
    val precipitationMm: Double?,
)
