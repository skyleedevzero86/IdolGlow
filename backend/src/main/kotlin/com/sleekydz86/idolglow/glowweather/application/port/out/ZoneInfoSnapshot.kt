package com.sleekydz86.idolglow.glowweather.application.port.out

data class ZoneInfoSnapshot(
    val regId: String,
    val regName: String?,
    val latitude: Double?,
    val longitude: Double?,
)
