package com.sleekydz86.idolglow.glowweather.domain

data class GlowWeatherRegion(
    val id: String,
    val name: String,
    val areaLabel: String,
    val latitude: Double,
    val longitude: Double,
    val asosStationId: Int,
    val midForecastStationId: Int,
    val midLandRegionId: String,
    val midTemperatureRegionId: String,
)
