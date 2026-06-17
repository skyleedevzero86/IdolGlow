package com.sleekydz86.idolglow.glowweather.application

data class GlowWeatherWindGuide(
    val directionDegrees: Int?,
    val directionLabel: String,
    val speedMps: Double?,
    val message: String,
    val referencePoints: List<GlowWeatherWindPoint>,
    val windFromClimateStatistics: Boolean = false,
    val climateStatisticsMonth: Int? = null,
)
