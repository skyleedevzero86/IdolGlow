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

data class CurrentObservationSnapshot(
    val observedAt: LocalDateTime?,
    val temperatureC: Double?,
    val humidity: Int?,
    val precipitationTypeCode: Int?,
    val windDirectionDeg: Int?,
    val windSpeedMps: Double?,
)

data class ShortForecastSnapshot(
    val forecastDateTime: LocalDateTime,
    val category: String,
    val value: String,
)

data class MidLandForecastSnapshot(
    val rainProbabilityByDay: Map<Int, Int>,
    val weatherByDay: Map<Int, String>,
)

data class MidTemperatureSnapshot(
    val minTempByDay: Map<Int, Double>,
    val maxTempByDay: Map<Int, Double>,
)

data class ZoneInfoSnapshot(
    val regId: String,
    val regName: String?,
    val latitude: Double?,
    val longitude: Double?,
)

data class AsosDailySnapshot(
    val date: LocalDate,
    val averageTempC: Double?,
    val precipitationMm: Double?,
)
