package com.sleekydz86.idolglow.glowweather.application.port.out

data class MidLandForecastSnapshot(
    val rainProbabilityByDay: Map<Int, Int>,
    val weatherByDay: Map<Int, String>,
)
