package com.sleekydz86.idolglow.glowweather.infrastructure

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.global.infrastructure.config.KmaWeatherProperties
import com.sleekydz86.idolglow.glowweather.application.port.out.AsosDailySnapshot
import com.sleekydz86.idolglow.glowweather.application.port.out.CurrentObservationSnapshot
import com.sleekydz86.idolglow.glowweather.application.port.out.GlowWeatherDataPort
import com.sleekydz86.idolglow.glowweather.application.port.out.MidLandForecastSnapshot
import com.sleekydz86.idolglow.glowweather.application.port.out.MidTemperatureSnapshot
import com.sleekydz86.idolglow.glowweather.application.port.out.ShortForecastSnapshot
import com.sleekydz86.idolglow.glowweather.application.port.out.ZoneInfoSnapshot
import com.sleekydz86.idolglow.glowweather.domain.GlowWeatherRegion
import com.sleekydz86.idolglow.glowweather.domain.KmaGridConverter
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriUtils
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.Duration
import java.time.format.DateTimeFormatter
import java.util.concurrent.atomic.AtomicLong

@Component
class KmaGlowWeatherClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val properties: KmaWeatherProperties,
) : GlowWeatherDataPort {
    private val log = LoggerFactory.getLogger(javaClass)
    private val unauthorizedBlockedUntilMillis = AtomicLong(0)

    override fun fetchUltraShortObservation(
        region: GlowWeatherRegion,
        baseDate: LocalDate,
        baseTime: LocalTime,
    ): CurrentObservationSnapshot? {
        val point = KmaGridConverter.toGrid(region.latitude, region.longitude)
        val items = fetchItems(
            "${properties.villageBaseUrl.trimEnd('/')}/getUltraSrtNcst",
            mapOf(
                "base_date" to baseDate.format(BASIC_DATE),
                "base_time" to baseTime.format(HHMM),
                "nx" to point.nx.toString(),
                "ny" to point.ny.toString(),
                "numOfRows" to "100",
            ),
        )
        if (items.isEmpty()) return null
        val byCategory = items.associateBy({ it.path("category").asText() }, { it.path("obsrValue").asText() })
        return CurrentObservationSnapshot(
            observedAt = LocalDateTime.of(baseDate, baseTime),
            temperatureC = byCategory["T1H"]?.toDoubleOrNull(),
            humidity = byCategory["REH"]?.toIntOrNull(),
            precipitationTypeCode = byCategory["PTY"]?.toIntOrNull(),
            windDirectionDeg = normalizeNullableInt(byCategory["VEC"]),
            windSpeedMps = normalizeNullableDouble(byCategory["WSD"]),
        )
    }

    override fun fetchUltraShortForecast(
        region: GlowWeatherRegion,
        baseDate: LocalDate,
        baseTime: LocalTime,
    ): List<ShortForecastSnapshot> {
        val point = KmaGridConverter.toGrid(region.latitude, region.longitude)
        val items = fetchItems(
            "${properties.villageBaseUrl.trimEnd('/')}/getUltraSrtFcst",
            mapOf(
                "base_date" to baseDate.format(BASIC_DATE),
                "base_time" to baseTime.format(HHMM),
                "nx" to point.nx.toString(),
                "ny" to point.ny.toString(),
                "numOfRows" to "120",
            ),
        )
        return items.mapNotNull(::toShortForecastSnapshot)
    }

    override fun fetchVillageForecast(
        region: GlowWeatherRegion,
        baseDate: LocalDate,
        baseTime: LocalTime,
    ): List<ShortForecastSnapshot> {
        val point = KmaGridConverter.toGrid(region.latitude, region.longitude)
        val items = fetchItems(
            "${properties.villageBaseUrl.trimEnd('/')}/getVilageFcst",
            mapOf(
                "base_date" to baseDate.format(BASIC_DATE),
                "base_time" to baseTime.format(HHMM),
                "nx" to point.nx.toString(),
                "ny" to point.ny.toString(),
                "numOfRows" to "1000",
            ),
        )
        return items.mapNotNull(::toShortForecastSnapshot)
    }

    override fun fetchMidOutlook(stationId: Int, tmFc: LocalDateTime): String? =
        fetchSingleItem(
            "${properties.midBaseUrl.trimEnd('/')}/getMidFcst",
            mapOf(
                "stnId" to stationId.toString(),
                "tmFc" to tmFc.format(MID_DATE_TIME),
            ),
        )?.path("wfSv")?.asText()?.trim()?.takeIf { it.isNotEmpty() }

    override fun fetchMidLandForecast(regionId: String, tmFc: LocalDateTime): MidLandForecastSnapshot? {
        val item = fetchSingleItem(
            "${properties.midBaseUrl.trimEnd('/')}/getMidLandFcst",
            mapOf(
                "regId" to regionId,
                "tmFc" to tmFc.format(MID_DATE_TIME),
            ),
        ) ?: return null

        val weather = linkedMapOf<Int, String>()
        val rain = linkedMapOf<Int, Int>()
        for (day in 4..10) {
            val weatherValue = when (day) {
                4 -> item.path("wf4Pm").asText().ifBlank { item.path("wf4Am").asText() }
                5 -> item.path("wf5Pm").asText().ifBlank { item.path("wf5Am").asText() }
                6 -> item.path("wf6Pm").asText().ifBlank { item.path("wf6Am").asText() }
                7 -> item.path("wf7Pm").asText().ifBlank { item.path("wf7Am").asText() }
                8 -> item.path("wf8").asText()
                9 -> item.path("wf9").asText()
                else -> item.path("wf10").asText()
            }.trim()
            val rainValue = when (day) {
                4 -> item.path("rnSt4Pm").asInt(item.path("rnSt4Am").asInt(-1))
                5 -> item.path("rnSt5Pm").asInt(item.path("rnSt5Am").asInt(-1))
                6 -> item.path("rnSt6Pm").asInt(item.path("rnSt6Am").asInt(-1))
                7 -> item.path("rnSt7Pm").asInt(item.path("rnSt7Am").asInt(-1))
                8 -> item.path("rnSt8").asInt(-1)
                9 -> item.path("rnSt9").asInt(-1)
                else -> item.path("rnSt10").asInt(-1)
            }
            if (weatherValue.isNotBlank()) weather[day] = weatherValue
            if (rainValue >= 0) rain[day] = rainValue
        }
        return MidLandForecastSnapshot(rainProbabilityByDay = rain, weatherByDay = weather)
    }

    override fun fetchMidTemperature(regionId: String, tmFc: LocalDateTime): MidTemperatureSnapshot? {
        val item = fetchSingleItem(
            "${properties.midBaseUrl.trimEnd('/')}/getMidTa",
            mapOf(
                "regId" to regionId,
                "tmFc" to tmFc.format(MID_DATE_TIME),
            ),
        ) ?: return null

        val minTemps = linkedMapOf<Int, Double>()
        val maxTemps = linkedMapOf<Int, Double>()
        for (day in 4..10) {
            normalizeNullableDouble(item.path("taMin$day").asText())?.let { minTemps[day] = it }
            normalizeNullableDouble(item.path("taMax$day").asText())?.let { maxTemps[day] = it }
        }
        return MidTemperatureSnapshot(minTempByDay = minTemps, maxTempByDay = maxTemps)
    }

    override fun fetchZoneInfo(regId: String): ZoneInfoSnapshot? {
        val item = fetchSingleItem(
            "${properties.zoneBaseUrl.trimEnd('/')}/getFcstZoneCd",
            mapOf("regId" to regId),
        ) ?: return null

        return ZoneInfoSnapshot(
            regId = item.path("regId").asText(regId),
            regName = item.path("regName").asText().ifBlank { null },
            latitude = normalizeNullableDouble(item.path("lat").asText()),
            longitude = normalizeNullableDouble(item.path("lon").asText()),
        )
    }

    override fun fetchAsosDaily(
        stationId: Int,
        startDate: LocalDate,
        endDate: LocalDate,
    ): List<AsosDailySnapshot> {
        val items = fetchItems(
            "${properties.asosBaseUrl.trimEnd('/')}/getWthrDataList",
            mapOf(
                "dataCd" to "ASOS",
                "dateCd" to "DAY",
                "startDt" to startDate.format(BASIC_DATE),
                "endDt" to endDate.format(BASIC_DATE),
                "stnIds" to stationId.toString(),
                "numOfRows" to "366",
            ),
        )
        return items.mapNotNull { item ->
            val date = runCatching { LocalDate.parse(item.path("tm").asText()) }.getOrNull() ?: return@mapNotNull null
            AsosDailySnapshot(
                date = date,
                averageTempC = normalizeNullableDouble(item.path("avgTa").asText()),
                precipitationMm = normalizeNullableDouble(item.path("sumRn").asText()),
            )
        }
    }

    private fun toShortForecastSnapshot(item: JsonNode): ShortForecastSnapshot? {
        val dateText = item.path("fcstDate").asText().trim()
        val timeText = item.path("fcstTime").asText().trim().padStart(4, '0')
        val category = item.path("category").asText().trim()
        val value = item.path("fcstValue").asText().trim()
        if (dateText.isEmpty() || timeText.isEmpty() || category.isEmpty()) return null
        val date = runCatching { LocalDate.parse(dateText, BASIC_DATE) }.getOrNull() ?: return null
        val time = runCatching { LocalTime.parse(timeText, HHMM) }.getOrNull() ?: return null
        return ShortForecastSnapshot(
            forecastDateTime = LocalDateTime.of(date, time),
            category = category,
            value = value,
        )
    }

    private fun fetchSingleItem(
        endpoint: String,
        params: Map<String, String>,
    ): JsonNode? = fetchItems(endpoint, params).firstOrNull()

    private fun fetchItems(
        endpoint: String,
        params: Map<String, String>,
    ): List<JsonNode> {
        if (isUnauthorizedBlocked()) return emptyList()
        val encodedKey = resolveEncodedServiceKey(properties.serviceKey) ?: return emptyList()
        val url = buildUrl(endpoint, encodedKey, params)
        return runCatching {
            val body = webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
                ?: return emptyList()
            val root = objectMapper.readTree(body)
            val response = root.path("response")
            val code = response.path("header").path("resultCode").asText()
            if (code != "00" && code != "0") {
                val message = response.path("header").path("resultMsg").asText()
                log.warn("KMA weather API returned code={} message={} endpoint={}", code, message, endpoint)
                return emptyList()
            }
            val itemNode = response.path("body").path("items").path("item")
            when {
                itemNode.isMissingNode || itemNode.isNull -> emptyList()
                itemNode.isArray -> itemNode.toList()
                itemNode.isObject -> listOf(itemNode)
                else -> emptyList()
            }
        }.getOrElse {
            handleFetchFailure(endpoint, it)
            emptyList()
        }
    }

    private fun handleFetchFailure(endpoint: String, throwable: Throwable) {
        when (throwable) {
            is WebClientResponseException.Unauthorized -> {
                if (markUnauthorizedCooldown()) {
                    log.warn(
                        "KMA weather API returned 401 Unauthorized for endpoint={}. Check KMA_WEATHER_SERVICE_KEY value/encoding. Fallback dashboard will be used for {} minutes.",
                        endpoint,
                        UNAUTHORIZED_COOLDOWN.toMinutes(),
                    )
                }
            }

            is WebClientResponseException -> {
                log.warn(
                    "Failed to call KMA weather endpoint={} status={} response={}",
                    endpoint,
                    throwable.statusCode.value(),
                    throwable.responseBodyAsString.take(200),
                )
            }

            else -> log.warn("Failed to call KMA weather endpoint={} message={}", endpoint, throwable.message)
        }
    }

    private fun buildUrl(
        endpoint: String,
        encodedServiceKey: String,
        params: Map<String, String>,
    ): String {
        val baseQuery = mutableListOf(
            "serviceKey=$encodedServiceKey",
            "pageNo=1",
            "dataType=JSON",
        )
        params.forEach { (key, value) ->
            val encoded = UriUtils.encodeQueryParam(value, StandardCharsets.UTF_8)
            baseQuery.add("$key=$encoded")
        }
        return "${endpoint.trim()}?${baseQuery.joinToString("&")}"
    }

    private fun resolveEncodedServiceKey(rawServiceKey: String): String? {
        val trimmed = rawServiceKey.trim()
            .removePrefix("serviceKey=")
            .removePrefix("KMA_WEATHER_SERVICE_KEY=")
            .removeSurrounding("\"")
        if (trimmed.isEmpty()) {
            log.info("KMA weather serviceKey is empty. Fallback dashboard will be used.")
            return null
        }
        return if (trimmed.contains('%')) trimmed else UriUtils.encodeQueryParam(trimmed, StandardCharsets.UTF_8)
    }

    private fun isUnauthorizedBlocked(): Boolean {
        val now = System.currentTimeMillis()
        val blockedUntil = unauthorizedBlockedUntilMillis.get()
        if (blockedUntil <= now) {
            if (blockedUntil != 0L) {
                unauthorizedBlockedUntilMillis.compareAndSet(blockedUntil, 0)
            }
            return false
        }
        return true
    }

    private fun markUnauthorizedCooldown(): Boolean {
        val now = System.currentTimeMillis()
        val blockedUntil = now + UNAUTHORIZED_COOLDOWN.toMillis()
        while (true) {
            val current = unauthorizedBlockedUntilMillis.get()
            if (current > now) return false
            if (unauthorizedBlockedUntilMillis.compareAndSet(current, blockedUntil)) return true
        }
    }

    private fun normalizeNullableInt(value: String?): Int? {
        val parsed = value?.trim()?.toDoubleOrNull()?.toInt()
        return parsed?.takeIf { kotlin.math.abs(it) < 900 }
    }

    private fun normalizeNullableDouble(value: String?): Double? {
        val parsed = value?.trim()?.toDoubleOrNull()
        return parsed?.takeIf { kotlin.math.abs(it) < 900.0 }
    }

    companion object {
        private val UNAUTHORIZED_COOLDOWN: Duration = Duration.ofMinutes(5)
        private val BASIC_DATE: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMdd")
        private val HHMM: DateTimeFormatter = DateTimeFormatter.ofPattern("HHmm")
        private val MID_DATE_TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    }
}
