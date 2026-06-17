package com.sleekydz86.idolglow.glowweather.application.port.out

data class MidTemperatureSnapshot(
    val minTempByDay: Map<Int, Double>,
    val maxTempByDay: Map<Int, Double>,
)
