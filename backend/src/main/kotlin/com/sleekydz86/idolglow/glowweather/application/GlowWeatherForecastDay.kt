package com.sleekydz86.idolglow.glowweather.application

import java.time.LocalDate

data class GlowWeatherForecastDay(
    val regionName: String,
    val date: LocalDate,
    val dateLabel: String,
    val dayLabel: String,
    val summary: String,
    val icon: String,
    val minTempC: Double?,
    val maxTempC: Double?,
    val precipitationChance: Int?,
    val windDirectionDegrees: Int?,
    val windDirectionLabel: String?,
    val windSpeedMps: Double?,
    val source: String,
)
