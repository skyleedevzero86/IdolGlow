package com.sleekydz86.idolglow.glowweather.application

data class GlowWeatherMonthlySummary(
    val monthLabel: String,
    val averageTemperatureC: Double?,
    val rainyDays: Int,
    val basedOn: String,
)
