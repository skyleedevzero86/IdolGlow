package com.sleekydz86.idolglow.glowweather.application

import com.sleekydz86.idolglow.glowweather.application.port.out.AsosDailySnapshot
import com.sleekydz86.idolglow.glowweather.application.port.out.CurrentObservationSnapshot
import com.sleekydz86.idolglow.glowweather.application.port.out.GlowWeatherDataPort
import com.sleekydz86.idolglow.glowweather.application.port.out.MidLandForecastSnapshot
import com.sleekydz86.idolglow.glowweather.application.port.out.MidTemperatureSnapshot
import com.sleekydz86.idolglow.glowweather.application.port.out.ShortForecastSnapshot
import com.sleekydz86.idolglow.glowweather.application.port.out.ZoneInfoSnapshot
import com.sleekydz86.idolglow.glowweather.domain.GlowWeatherRegion
import com.sleekydz86.idolglow.glowweather.domain.GlowWeatherRegions
import com.sleekydz86.idolglow.glowweather.domain.WindDirection
import com.sleekydz86.idolglow.glowweather.infrastructure.GlowClimateWindRepository
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.round

@Service
class GlowWeatherQueryService(
    private val glowWeatherDataPort: GlowWeatherDataPort,
    private val glowClimateWindRepository: GlowClimateWindRepository,
) {
    private val zoneId: ZoneId = ZoneId.of("Asia/Seoul")

    fun dashboard(regionId: String?): GlowWeatherDashboardResponse {
        val region = GlowWeatherRegions.find(regionId)
        val now = ZonedDateTime.now(zoneId)
        val currentObservation = runCatching {
            val base = resolveUltraShortObservationBase(now)
            glowWeatherDataPort.fetchUltraShortObservation(region, base.first, base.second)
        }.getOrNull()
        val ultraShortForecast = runCatching {
            val base = resolveUltraShortForecastBase(now)
            glowWeatherDataPort.fetchUltraShortForecast(region, base.first, base.second)
        }.getOrElse { emptyList() }
        val villageForecast = runCatching {
            val base = resolveVillageForecastBase(now)
            glowWeatherDataPort.fetchVillageForecast(region, base.first, base.second)
        }.getOrElse { emptyList() }
        val midBase = resolveMidForecastBase(now)
        val midLand = runCatching { glowWeatherDataPort.fetchMidLandForecast(region.midLandRegionId, midBase) }.getOrNull()
        val midTemp = runCatching { glowWeatherDataPort.fetchMidTemperature(region.midTemperatureRegionId, midBase) }.getOrNull()
        val zoneInfo = runCatching { glowWeatherDataPort.fetchZoneInfo(region.midTemperatureRegionId) }.getOrNull()
        val outlook = runCatching { glowWeatherDataPort.fetchMidOutlook(region.midForecastStationId, midBase) }.getOrNull()
        val asosDaily = fetchMonthlyAsos(region, now.toLocalDate())

        val builtForecast = buildForecast(region.name, now.toLocalDate(), villageForecast, midLand, midTemp)
        val forecastFromApi = hasForecastMeasurements(builtForecast)
        val forecast = if (forecastFromApi) builtForecast else fallbackForecast(region, now.toLocalDate())
        val currentFromApi = currentObservation != null
        val fallbackCurrentFromForecast = buildFallbackCurrent(region, now.toLocalDate(), ultraShortForecast, forecast)
        val current = buildCurrentResponse(region, zoneInfo, currentObservation, fallbackCurrentFromForecast)
        val monthlySummary = buildMonthlySummary(region, now.toLocalDate(), asosDaily, forecast)
        val outlookSummary = when {
            !outlook.isNullOrBlank() -> outlook
            !forecastFromApi -> "${region.name} 기준 최근 예보를 바탕으로 화면을 구성했어요."
            else -> ""
        }
        val recommendations = buildRecommendations(region, current, forecast, monthlySummary, outlookSummary)
        val windGuide = buildWindGuide(region, now, current, forecast)

        return GlowWeatherDashboardResponse(
            selectedRegionId = region.id,
            regions = GlowWeatherRegions.all.map { candidate ->
                GlowWeatherRegionSummary(
                    id = candidate.id,
                    name = candidate.name,
                    areaLabel = candidate.areaLabel,
                )
            },
            region = GlowWeatherSelectedRegion(
                id = region.id,
                name = zoneInfo?.regName ?: region.name,
                areaLabel = region.areaLabel,
                latitude = zoneInfo?.latitude ?: region.latitude,
                longitude = zoneInfo?.longitude ?: region.longitude,
            ),
            current = current,
            monthlySummary = monthlySummary,
            outlookSummary = outlookSummary,
            forecast = forecast,
            recommendations = recommendations,
            windGuide = windGuide,
            forecastFromApi = forecastFromApi,
            currentFromApi = currentFromApi,
            generatedAt = now.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME),
        )
    }

    private fun fetchMonthlyAsos(region: GlowWeatherRegion, today: LocalDate): List<AsosDailySnapshot> {
        val endDate = today.minusDays(1)
        if (endDate.month != today.month || endDate.year != today.year) return emptyList()
        val startDate = YearMonth.from(today).atDay(1)
        return runCatching {
            glowWeatherDataPort.fetchAsosDaily(region.asosStationId, startDate, endDate)
        }.getOrElse { emptyList() }
    }

    private fun buildCurrentResponse(
        region: GlowWeatherRegion,
        zoneInfo: ZoneInfoSnapshot?,
        observation: CurrentObservationSnapshot?,
        fallback: CurrentWeatherResponse,
    ): CurrentWeatherResponse {
        val base = observation ?: return fallback
        return fallback.copy(
            regionName = zoneInfo?.regName ?: region.name,
            observedAt = base.observedAt?.format(DISPLAY_DATE_TIME) ?: fallback.observedAt,
            temperatureC = base.temperatureC ?: fallback.temperatureC,
            humidity = base.humidity ?: fallback.humidity,
            precipitationLabel = precipitationTypeLabel(base.precipitationTypeCode) ?: fallback.precipitationLabel,
            windDirectionDegrees = base.windDirectionDeg ?: fallback.windDirectionDegrees,
            windDirectionLabel = WindDirection.to16Point(base.windDirectionDeg).takeIf { it != "-" } ?: fallback.windDirectionLabel,
            windSpeedMps = base.windSpeedMps ?: fallback.windSpeedMps,
        )
    }

    private fun buildFallbackCurrent(
        region: GlowWeatherRegion,
        today: LocalDate,
        ultraShortForecast: List<ShortForecastSnapshot>,
        forecast: List<GlowWeatherForecastDay>,
    ): CurrentWeatherResponse {
        val grouped = ultraShortForecast.groupBy { it.forecastDateTime }
        val nearest = grouped.keys.sorted().firstOrNull()
        val fallbackDay = forecast.firstOrNull() ?: fallbackForecast(region, today).first()
        val entries = nearest?.let { grouped[it].orEmpty() } ?: emptyList()

        fun value(category: String): String? = entries.firstOrNull { it.category == category }?.value

        val temperature = value("T1H")?.toDoubleOrNull() ?: fallbackDay.minTempC?.plus(fallbackDay.maxTempC ?: 0.0)?.div(2.0)
        val ptyCode = value("PTY")?.toIntOrNull()
        val skyCode = value("SKY")?.toIntOrNull()
        val windDeg = value("VEC")?.toDoubleOrNull()?.roundToIntSafe()
        val windSpeed = value("WSD")?.toDoubleOrNull()

        return CurrentWeatherResponse(
            regionName = fallbackDay.regionName,
            observedAt = nearest?.format(DISPLAY_DATE_TIME) ?: today.atTime(9, 0).format(DISPLAY_DATE_TIME),
            temperatureC = temperature,
            humidity = value("REH")?.toIntOrNull(),
            skyLabel = skyLabel(skyCode, ptyCode) ?: fallbackDay.summary,
            precipitationLabel = precipitationTypeLabel(ptyCode) ?: if ((fallbackDay.precipitationChance ?: 0) >= 60) "비 가능성 높음" else "강수 없음",
            windDirectionDegrees = windDeg,
            windDirectionLabel = WindDirection.to16Point(windDeg),
            windSpeedMps = windSpeed,
        )
    }

    private fun buildForecast(
        regionName: String,
        startDate: LocalDate,
        shortForecast: List<ShortForecastSnapshot>,
        midLand: MidLandForecastSnapshot?,
        midTemp: MidTemperatureSnapshot?,
    ): List<GlowWeatherForecastDay> {
        val shortMap = shortForecast
            .groupBy { it.forecastDateTime.toLocalDate() }
            .mapValues { (_, items) -> toDailyForecastFromShort(items) }

        return (0 until 10).map { offset ->
            val date = startDate.plusDays(offset.toLong())
            val fromShort = shortMap[date]
            if (fromShort != null && offset <= 4) {
                fromShort.copy(regionName = regionName, date = date, dateLabel = date.format(DATE_LABEL), dayLabel = dayLabel(date))
            } else {
                val dayIndex = offset
                val weather = midLand?.weatherByDay?.get(dayIndex)
                val rain = midLand?.rainProbabilityByDay?.get(dayIndex)
                val minTemp = midTemp?.minTempByDay?.get(dayIndex)
                val maxTemp = midTemp?.maxTempByDay?.get(dayIndex)
                GlowWeatherForecastDay(
                    regionName = regionName,
                    date = date,
                    dateLabel = date.format(DATE_LABEL),
                    dayLabel = dayLabel(date),
                    summary = weather ?: fromShort?.summary ?: "구름많음",
                    icon = if (weather != null) forecastIcon(weather, null, null) else fromShort?.icon ?: "cloud",
                    minTempC = minTemp ?: fromShort?.minTempC,
                    maxTempC = maxTemp ?: fromShort?.maxTempC,
                    precipitationChance = rain ?: fromShort?.precipitationChance,
                    windDirectionDegrees = fromShort?.windDirectionDegrees,
                    windDirectionLabel = fromShort?.windDirectionLabel,
                    windSpeedMps = fromShort?.windSpeedMps,
                    source = if (weather != null || minTemp != null || maxTemp != null) "mid" else "fallback",
                )
            }
        }
    }

    private fun toDailyForecastFromShort(items: List<ShortForecastSnapshot>): GlowWeatherForecastDay {
        val grouped = items.groupBy { it.category }
        val tmpValues = grouped["TMP"].orEmpty().mapNotNull { it.value.toDoubleOrNull() }
        val minTemp = grouped["TMN"].orEmpty().firstNotNullOfOrNull { it.value.toDoubleOrNull() } ?: tmpValues.minOrNull()
        val maxTemp = grouped["TMX"].orEmpty().firstNotNullOfOrNull { it.value.toDoubleOrNull() } ?: tmpValues.maxOrNull()
        val skyCode = chooseNearest(grouped["SKY"].orEmpty(), 12)?.value?.toIntOrNull()
        val ptyCode = chooseNearest(grouped["PTY"].orEmpty(), 12)?.value?.toIntOrNull()
        val pop = grouped["POP"].orEmpty().mapNotNull { it.value.toIntOrNull() }.maxOrNull()
        val windDirection = chooseNearest(grouped["VEC"].orEmpty(), 12)?.value?.toDoubleOrNull()?.roundToIntSafe()
        val windSpeed = chooseNearest(grouped["WSD"].orEmpty(), 12)?.value?.toDoubleOrNull()
        val summary = skyLabel(skyCode, ptyCode) ?: "구름많음"
        val date = items.minOf { it.forecastDateTime.toLocalDate() }
        return GlowWeatherForecastDay(
            regionName = "",
            date = date,
            dateLabel = date.format(DATE_LABEL),
            dayLabel = dayLabel(date),
            summary = summary,
            icon = forecastIcon(null, skyCode, ptyCode),
            minTempC = minTemp,
            maxTempC = maxTemp,
            precipitationChance = pop,
            windDirectionDegrees = windDirection,
            windDirectionLabel = WindDirection.to16Point(windDirection),
            windSpeedMps = windSpeed,
            source = "short",
        )
    }

    private fun chooseNearest(items: List<ShortForecastSnapshot>, hour: Int): ShortForecastSnapshot? =
        items.minByOrNull { kotlin.math.abs(it.forecastDateTime.hour - hour) }

    private fun buildMonthlySummary(
        region: GlowWeatherRegion,
        today: LocalDate,
        asos: List<AsosDailySnapshot>,
        forecast: List<GlowWeatherForecastDay>,
    ): GlowWeatherMonthlySummary {
        val monthLabel = "${today.monthValue}월"
        if (asos.isNotEmpty()) {
            val temps = asos.mapNotNull { it.averageTempC }
            val rainyDays = asos.count { (it.precipitationMm ?: 0.0) > 0.0 }
            return GlowWeatherMonthlySummary(
                monthLabel = monthLabel,
                averageTemperatureC = temps.averageOrNull(),
                rainyDays = rainyDays,
                basedOn = "${region.name} ASOS 일자료 기준",
            )
        }

        val temps = forecast.take(5).mapNotNull {
            val min = it.minTempC
            val max = it.maxTempC
            if (min == null || max == null) null else (min + max) / 2.0
        }
        return GlowWeatherMonthlySummary(
            monthLabel = monthLabel,
            averageTemperatureC = temps.averageOrNull(),
            rainyDays = forecast.take(10).count { (it.precipitationChance ?: 0) >= 60 },
            basedOn = "예보 데이터 추정치",
        )
    }

    private fun buildRecommendations(
        region: GlowWeatherRegion,
        current: CurrentWeatherResponse,
        forecast: List<GlowWeatherForecastDay>,
        monthlySummary: GlowWeatherMonthlySummary,
        outlookSummary: String?,
    ): List<GlowWeatherRecommendation> {
        val today = forecast.firstOrNull()
        val tomorrow = forecast.getOrNull(1)
        val warmest = forecast.mapNotNull { it.maxTempC }.maxOrNull()
        val coldest = forecast.mapNotNull { it.minTempC }.minOrNull()

        val activityTone = when {
            (today?.precipitationChance ?: 0) >= 60 -> "rain"
            (today?.maxTempC ?: 0.0) >= 24.0 -> "sunny"
            else -> "mint"
        }
        val activityTitle = when (activityTone) {
            "rain" -> "비 예보가 있어요"
            "sunny" -> "날씨도 좋아요"
            else -> "산책하기 무난해요"
        }
        val activitySubtitle = when (activityTone) {
            "rain" -> "실내 일정이나 우산 준비를 먼저 챙겨보세요."
            "sunny" -> "바깥 일정 잡기 좋은 컨디션이에요."
            else -> "이 시간에 어떻게 플레이하나요?"
        }

        val outfitTitle = when {
            warmest != null && warmest >= 27.0 -> "가벼운 옷차림이 잘 맞아요"
            coldest != null && coldest <= 8.0 -> "겉옷을 챙기는 편이 좋아요"
            else -> "한국에서 어떻게 옷을 입어야 할까요?"
        }
        val outfitSubtitle = when {
            warmest != null && warmest >= 27.0 -> "얇은 셔츠, 반팔, 자외선 대비 아이템을 추천해요."
            coldest != null && coldest <= 8.0 -> "아침저녁 기온 차가 커서 가벼운 아우터가 좋아요."
            else -> "${region.name} ${monthlySummary.monthLabel} 기준 계절감에 맞춘 의상 가이드예요."
        }

        return listOf(
            GlowWeatherRecommendation(
                id = "activity",
                icon = "sun",
                tone = activityTone,
                title = activityTitle,
                subtitle = activitySubtitle,
                description = outlookSummary?.takeIf { it.isNotBlank() } ?: "최근 예보를 바탕으로 야외 일정 감각을 정리했어요.",
            ),
            GlowWeatherRecommendation(
                id = "umbrella",
                icon = "rain",
                tone = if ((tomorrow?.precipitationChance ?: 0) >= 60) "teal" else "sky",
                title = if ((tomorrow?.precipitationChance ?: 0) >= 60) "비/눈 가능성을 확인해보세요" else "강수 가능성은 높지 않아요",
                subtitle = if ((tomorrow?.precipitationChance ?: 0) >= 60) "내일 일정이라면 우산이나 방수 아이템이 있으면 편해요." else "짧은 외출이라면 가볍게 움직여도 괜찮아요.",
                description = "강수확률, 하늘상태, 풍속을 함께 보고 일정을 조정해보세요.",
            ),
            GlowWeatherRecommendation(
                id = "outfit",
                icon = "shirt",
                tone = "blue",
                title = outfitTitle,
                subtitle = outfitSubtitle,
                description = "최저·최고 기온과 바람을 함께 반영한 간단한 복장 가이드예요.",
            ),
        )
    }

    private fun buildWindGuide(
        region: GlowWeatherRegion,
        now: ZonedDateTime,
        current: CurrentWeatherResponse,
        forecast: List<GlowWeatherForecastDay>,
    ): GlowWeatherWindGuide {
        val liveDeg = current.windDirectionDegrees ?: forecast.firstNotNullOfOrNull { it.windDirectionDegrees }
        val liveSpeed = current.windSpeedMps ?: forecast.firstNotNullOfOrNull { it.windSpeedMps }
        val liveComplete = liveDeg != null && liveSpeed != null
        val month = now.monthValue
        val climateCell = glowClimateWindRepository.month(region.id, month)

        val referencePoints = WindDirection.referencePoints().map { (label, degree) ->
            GlowWeatherWindPoint(label, degree)
        }

        if (liveComplete) {
            val degrees = liveDeg!!
            val speed = liveSpeed!!
            val direction = current.windDirectionLabel.takeIf { it.isNotBlank() }
                ?: WindDirection.to16Point(degrees).takeIf { it != "-" } ?: "-"
            val message = when {
                speed >= 9.0 -> "강한 바람 구간에 가까워요. 가벼운 겉옷이나 모자 고정을 신경 써주세요."
                speed >= 4.0 -> "약간 바람이 느껴질 수 있어요. 야외 이동 시 체감온도가 내려갈 수 있어요."
                else -> "바람은 비교적 잔잔한 편이에요."
            }
            return GlowWeatherWindGuide(
                directionDegrees = degrees,
                directionLabel = direction,
                speedMps = speed,
                message = message,
                referencePoints = referencePoints,
                windFromClimateStatistics = false,
                climateStatisticsMonth = null,
            )
        }

        if (climateCell != null) {
            val deg = WindDirection.degreesFromCompassAbbreviation(climateCell.dir)
            val direction = when {
                deg != null -> WindDirection.to16Point(deg).takeIf { it != "-" } ?: climateCell.dir.trim().uppercase()
                else -> climateCell.dir.trim().uppercase()
            }
            val message =
                "실시간 풍향·풍속 대신, 이 지역·이 달 기후통계(최다풍향·평균풍속)을 보여드려요."
            return GlowWeatherWindGuide(
                directionDegrees = deg,
                directionLabel = direction,
                speedMps = climateCell.mps,
                message = message,
                referencePoints = referencePoints,
                windFromClimateStatistics = true,
                climateStatisticsMonth = month,
            )
        }

        val degrees = liveDeg
        val direction = current.windDirectionLabel.ifBlank { WindDirection.to16Point(degrees) }
        val speed = liveSpeed
        val message = when {
            speed == null -> "풍향 데이터가 없어서 예보 기준으로만 보여드리고 있어요."
            speed >= 9.0 -> "강한 바람 구간에 가까워요. 가벼운 겉옷이나 모자 고정을 신경 써주세요."
            speed >= 4.0 -> "약간 바람이 느껴질 수 있어요. 야외 이동 시 체감온도가 내려갈 수 있어요."
            else -> "바람은 비교적 잔잔한 편이에요."
        }
        return GlowWeatherWindGuide(
            directionDegrees = degrees,
            directionLabel = direction,
            speedMps = speed,
            message = message,
            referencePoints = referencePoints,
            windFromClimateStatistics = false,
            climateStatisticsMonth = null,
        )
    }

    private fun hasForecastMeasurements(forecast: List<GlowWeatherForecastDay>): Boolean =
        forecast.any {
            it.minTempC != null ||
                it.maxTempC != null ||
                it.precipitationChance != null ||
                it.windDirectionDegrees != null ||
                it.windSpeedMps != null
        }

    private fun fallbackForecast(region: GlowWeatherRegion, today: LocalDate): List<GlowWeatherForecastDay> {
        val bias = fallbackTemperatureBias(region)
        return (0 until 10).map { index ->
            val date = today.plusDays(index.toLong())
            GlowWeatherForecastDay(
                regionName = region.name,
                date = date,
                dateLabel = date.format(DATE_LABEL),
                dayLabel = dayLabel(date),
                summary = when (index % 4) {
                    0 -> "맑음"
                    1 -> "구름많음"
                    2 -> "흐림"
                    else -> "구름많고 비"
                },
                icon = when (index % 4) {
                    0 -> "sun"
                    1 -> "partly"
                    2 -> "cloud"
                    else -> "rain"
                },
                minTempC = 11.0 + index + bias,
                maxTempC = 19.0 + index + bias,
                precipitationChance = if (index % 4 == 3) 70 else 20,
                windDirectionDegrees = 90 + (index * 10),
                windDirectionLabel = WindDirection.to16Point(90 + (index * 10)),
                windSpeedMps = 2.5 + (index % 3),
                source = "fallback",
            )
        }
    }

    private fun fallbackTemperatureBias(region: GlowWeatherRegion): Double {
        val h = region.id.fold(0) { acc, ch -> 31 * acc + ch.code }
        return ((h % 13) - 6) * 0.5
    }

    private fun skyLabel(skyCode: Int?, ptyCode: Int?): String? {
        val precipitation = precipitationTypeLabel(ptyCode)
        if (precipitation != null && precipitation != "없음") return precipitation
        return when (skyCode) {
            1 -> "맑음"
            3 -> "구름많음"
            4 -> "흐림"
            else -> null
        }
    }

    private fun precipitationTypeLabel(code: Int?): String? =
        when (code) {
            0 -> "없음"
            1 -> "비"
            2 -> "비/눈"
            3 -> "눈"
            4 -> "소나기"
            5 -> "빗방울"
            6 -> "빗방울눈날림"
            7 -> "눈날림"
            else -> null
        }

    private fun forecastIcon(weather: String?, skyCode: Int?, ptyCode: Int?): String {
        val label = weather.orEmpty()
        return when {
            ptyCode == 1 || label.contains("비") || label.contains("소나기") -> "rain"
            ptyCode == 2 || ptyCode == 3 || label.contains("눈") -> "snow"
            skyCode == 1 || label.contains("맑음") -> "sun"
            skyCode == 3 || label.contains("구름많") -> "partly"
            else -> "cloud"
        }
    }

    private fun resolveVillageForecastBase(now: ZonedDateTime): Pair<LocalDate, LocalTime> {
        val cycles = listOf(2, 5, 8, 11, 14, 17, 20, 23)
        val adjusted = now.minusMinutes(10)
        val hour = cycles.lastOrNull { it <= adjusted.hour } ?: 23
        val date = if (adjusted.hour >= 2) adjusted.toLocalDate() else adjusted.minusDays(1).toLocalDate()
        return date to LocalTime.of(hour, 0)
    }

    private fun resolveUltraShortObservationBase(now: ZonedDateTime): Pair<LocalDate, LocalTime> {
        val adjusted = now.minusMinutes(10)
        return adjusted.toLocalDate() to LocalTime.of(adjusted.hour, 0)
    }

    private fun resolveUltraShortForecastBase(now: ZonedDateTime): Pair<LocalDate, LocalTime> {
        val adjusted = now.minusMinutes(45)
        return adjusted.toLocalDate() to LocalTime.of(adjusted.hour, 30)
    }

    private fun resolveMidForecastBase(now: ZonedDateTime): LocalDateTime {
        val today = now.toLocalDate()
        return when {
            now.hour >= 18 -> LocalDateTime.of(today, LocalTime.of(18, 0))
            now.hour >= 6 -> LocalDateTime.of(today, LocalTime.of(6, 0))
            else -> LocalDateTime.of(today.minusDays(1), LocalTime.of(18, 0))
        }
    }

    private fun dayLabel(date: LocalDate): String =
        when (date.dayOfWeek.value) {
            1 -> "월"
            2 -> "화"
            3 -> "수"
            4 -> "목"
            5 -> "금"
            6 -> "토"
            else -> "일"
        }

    private fun Double.round1(): Double = round(this * 10.0) / 10.0

    private fun Iterable<Double>.averageOrNull(): Double? {
        val values = toList()
        if (values.isEmpty()) return null
        return (values.average()).round1()
    }

    private fun Double.roundToIntSafe(): Int = round(this).toInt()

    companion object {
        private val DATE_LABEL: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd")
        private val DISPLAY_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm")
    }
}

data class GlowWeatherDashboardResponse(
    val selectedRegionId: String,
    val regions: List<GlowWeatherRegionSummary>,
    val region: GlowWeatherSelectedRegion,
    val current: CurrentWeatherResponse,
    val monthlySummary: GlowWeatherMonthlySummary,
    val outlookSummary: String,
    val forecast: List<GlowWeatherForecastDay>,
    val recommendations: List<GlowWeatherRecommendation>,
    val windGuide: GlowWeatherWindGuide,
    val forecastFromApi: Boolean,
    val currentFromApi: Boolean,
    val generatedAt: String,
)

data class GlowWeatherRegionSummary(
    val id: String,
    val name: String,
    val areaLabel: String,
)

data class GlowWeatherSelectedRegion(
    val id: String,
    val name: String,
    val areaLabel: String,
    val latitude: Double,
    val longitude: Double,
)

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

data class GlowWeatherMonthlySummary(
    val monthLabel: String,
    val averageTemperatureC: Double?,
    val rainyDays: Int,
    val basedOn: String,
)

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

data class GlowWeatherRecommendation(
    val id: String,
    val icon: String,
    val tone: String,
    val title: String,
    val subtitle: String,
    val description: String,
)

data class GlowWeatherWindGuide(
    val directionDegrees: Int?,
    val directionLabel: String,
    val speedMps: Double?,
    val message: String,
    val referencePoints: List<GlowWeatherWindPoint>,
    val windFromClimateStatistics: Boolean = false,
    val climateStatisticsMonth: Int? = null,
)

data class GlowWeatherWindPoint(
    val label: String,
    val degrees: Int,
)
