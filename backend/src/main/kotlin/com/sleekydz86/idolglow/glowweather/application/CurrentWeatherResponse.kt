package com.sleekydz86.idolglow.glowweather.application

data class CurrentWeatherResponse(
    val regionName: String,
    val observedAt: String,
    val temperatureC: Double?,
    val humidity: Int?,
    val skyLabel: String = "구름많음",
    val precipitationLabel: String = "강수 없음",
    val windDirectionDegrees: Int?,
    val windDirectionLabel: String,
    val windSpeedMps: Double?,
)
