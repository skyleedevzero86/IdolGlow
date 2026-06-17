package com.sleekydz86.idolglow.glowweather.application.port.out

import java.time.LocalDateTime

data class ShortForecastSnapshot(
    val forecastDateTime: LocalDateTime,
    val category: String,
    val value: String,
)
