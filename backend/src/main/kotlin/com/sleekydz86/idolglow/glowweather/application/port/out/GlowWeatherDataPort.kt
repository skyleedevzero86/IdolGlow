package com.sleekydz86.idolglow.glowweather.application.port.out

import com.sleekydz86.idolglow.glowweather.domain.GlowWeatherRegion
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

interface GlowWeatherDataPort {
    fun fetchUltraShortObservation(
        region: GlowWeatherRegion,
        baseDate: LocalDate,
        baseTime: LocalTime,
    ): CurrentObservationSnapshot?

    fun fetchUltraShortForecast(
        region: GlowWeatherRegion,
        baseDate: LocalDate,
        baseTime: LocalTime,
    ): List<ShortForecastSnapshot>

    fun fetchVillageForecast(
        region: GlowWeatherRegion,
        baseDate: LocalDate,
        baseTime: LocalTime,
    ): List<ShortForecastSnapshot>

    fun fetchMidOutlook(
        stationId: Int,
        tmFc: LocalDateTime,
    ): String?

    fun fetchMidLandForecast(
        regionId: String,
        tmFc: LocalDateTime,
    ): MidLandForecastSnapshot?

    fun fetchMidTemperature(
        regionId: String,
        tmFc: LocalDateTime,
    ): MidTemperatureSnapshot?

    fun fetchZoneInfo(regId: String): ZoneInfoSnapshot?

    fun fetchAsosDaily(
        stationId: Int,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<AsosDailySnapshot>
}
