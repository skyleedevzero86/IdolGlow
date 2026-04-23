package com.sleekydz86.idolglow.productpackage.attraction.infrastructure

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import tools.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.global.infrastructure.config.TourApiProperties
import com.sleekydz86.idolglow.global.infrastructure.exception.CustomException
import com.sleekydz86.idolglow.global.infrastructure.exception.tour.TourAttractionExceptionType
import com.sleekydz86.idolglow.productpackage.attraction.application.port.out.TourAttractionQueryPort
import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

@Component
class TourApiClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val props: TourApiProperties,
) : TourAttractionQueryPort {
    private val log = LoggerFactory.getLogger(this::class.java)
    private val cache = ConcurrentHashMap<TourApiCacheKey, TourApiCacheEntry>()

    override fun fetchAreaBasedAttractions(
        baseYm: String,
        areaCode: Int,
        signguCode: Int,
        size: Int,
    ): List<TourAttraction> {
        val key = TourApiCacheKey(baseYm, areaCode, signguCode, size)
        val now = Instant.now()
        val cached = cache[key]
        if (cached != null && cached.expireAt.isAfter(now)) {
            return cached.attractions
        }

        val normalizedServiceKey = normalizeServiceKey(props.serviceKey)
        if (normalizedServiceKey.isEmpty()) {
            throw CustomException(TourAttractionExceptionType.TOUR_API_KEY_MISSING)
        }

        val rawResponse = requestWithFallback(
            normalizedServiceKey = normalizedServiceKey,
            baseYm = baseYm,
            areaCode = areaCode,
            signguCode = signguCode,
            requestedSize = size,
            key = key,
        )

        if (!rawResponse.statusCode.is2xxSuccessful) {
            return staleOrThrow(key, TourAttractionExceptionType.TOUR_API_CALL_FAILED)
        }

        val parsed = parseJsonResponse(rawResponse.body)
        if (parsed.response?.header?.resultCode != "0000") {
            return staleOrThrow(key, TourAttractionExceptionType.TOUR_API_ERROR)
        }

        val attractions = parsed.response?.body?.items?.item.orEmpty().mapNotNull { item ->
            val name = item.hubTatsNm?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val attractionCode = item.hubTatsCd?.trim()?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
            val rank = item.hubRank?.toIntOrNull() ?: Int.MAX_VALUE
            TourAttraction(
                attractionCode = attractionCode,
                name = name,
                areaCode = item.areaCd?.toIntOrNull() ?: areaCode,
                areaName = item.areaNm,
                signguCode = item.signguCd?.toIntOrNull() ?: signguCode,
                signguName = item.signguNm,
                categoryLarge = item.hubCtgryLclsNm,
                categoryMiddle = item.hubCtgryMclsNm,
                rank = rank,
                mapX = item.mapX?.toDoubleOrNull(),
                mapY = item.mapY?.toDoubleOrNull(),
                baseYm = item.baseYm ?: baseYm,
            )
        }

        putCache(key, attractions, now)
        return attractions
    }

    private fun parseJsonResponse(body: String): TourApiResponseEnvelope {
        return try {
            objectMapper.readValue(body, TourApiResponseEnvelope::class.java)
        } catch (_: Exception) {
            throw CustomException(TourAttractionExceptionType.TOUR_API_BAD_RESPONSE)
        }
    }

    private fun endpointUrl(): String = "${props.baseUrl.trimEnd('/')}/areaBasedList1"

    private fun requestWithFallback(
        normalizedServiceKey: String,
        baseYm: String,
        areaCode: Int,
        signguCode: Int,
        requestedSize: Int,
        key: TourApiCacheKey,
    ): RawTourApiResponse {
        val primarySize = requestedSize.coerceIn(1, 1000)
        val primary = requestRawResponse(
            normalizedServiceKey = normalizedServiceKey,
            baseYm = baseYm,
            areaCode = areaCode,
            signguCode = signguCode,
            numOfRows = primarySize,
        )

        if (primary.statusCode.is2xxSuccessful) {
            return primary
        }

        log.warn(
            "Tour API 비정상 응답. status={}, numOfRows={}, body={}",
            primary.statusCode.value(),
            primarySize,
            primary.body.take(300),
        )

        val fallbackSize = 100
        if (primarySize <= fallbackSize) {
            staleOrThrow(key, TourAttractionExceptionType.TOUR_API_CALL_FAILED)
            throw CustomException(TourAttractionExceptionType.TOUR_API_CALL_FAILED)
        }

        val fallback = requestRawResponse(
            normalizedServiceKey = normalizedServiceKey,
            baseYm = baseYm,
            areaCode = areaCode,
            signguCode = signguCode,
            numOfRows = fallbackSize,
        )
        if (fallback.statusCode.is2xxSuccessful) {
            log.info("Tour API 재시도 성공. numOfRows={} -> {}", primarySize, fallbackSize)
            return fallback
        }

        log.warn(
            "Tour API 재시도도 실패. status={}, numOfRows={}, body={}",
            fallback.statusCode.value(),
            fallbackSize,
            fallback.body.take(300),
        )
        staleOrThrow(key, TourAttractionExceptionType.TOUR_API_CALL_FAILED)
        throw CustomException(TourAttractionExceptionType.TOUR_API_CALL_FAILED)
    }

    private fun requestRawResponse(
        normalizedServiceKey: String,
        baseYm: String,
        areaCode: Int,
        signguCode: Int,
        numOfRows: Int,
    ): RawTourApiResponse {
        return try {
            webClient.get()
                .uri(endpointUrl()) { uriBuilder ->
                    uriBuilder
                        .queryParam("serviceKey", normalizedServiceKey)
                        .queryParam("pageNo", 1)
                        .queryParam("numOfRows", numOfRows)
                        .queryParam("MobileOS", props.mobileOs)
                        .queryParam("MobileApp", props.mobileApp)
                        .queryParam("baseYm", baseYm)
                        .queryParam("areaCd", areaCode)
                        .queryParam("signguCd", signguCode)
                        .queryParam("_type", "json")
                        .build()
                }
                .exchangeToMono { clientResponse ->
                    clientResponse.bodyToMono(String::class.java)
                        .defaultIfEmpty("")
                        .map { body -> RawTourApiResponse(clientResponse.statusCode(), body) }
                }
                .block()
                ?: RawTourApiResponse(HttpStatusCode.valueOf(502), "")
        } catch (exception: Exception) {
            log.warn(
                "Tour API 호출 예외. baseYm={}, areaCode={}, signguCode={}, numOfRows={}, message={}",
                baseYm,
                areaCode,
                signguCode,
                numOfRows,
                exception.message
            )
            RawTourApiResponse(HttpStatusCode.valueOf(502), exception.message ?: "")
        }
    }

    private fun normalizeServiceKey(rawServiceKey: String): String {
        val trimmed = rawServiceKey.trim()
        if (trimmed.isEmpty()) return ""
        return if (trimmed.contains('%')) URLDecoder.decode(trimmed, StandardCharsets.UTF_8) else trimmed
    }

    private fun staleOrThrow(key: TourApiCacheKey, causeType: TourAttractionExceptionType): List<TourAttraction> {
        val stale = cache[key]
        if (stale != null) {
            log.warn("Tour API 호출 실패로 오래된 캐시를 사용합니다. key={}, cachedAt={}", key, stale.cachedAt)
            return stale.attractions
        }
        throw CustomException(causeType)
    }

    private fun putCache(key: TourApiCacheKey, attractions: List<TourAttraction>, now: Instant) {
        val ttlMinutes = props.cacheTtlMinutes.coerceAtLeast(1)
        cache[key] = TourApiCacheEntry(
            attractions = attractions,
            cachedAt = now,
            expireAt = now.plusSeconds(Duration.ofMinutes(ttlMinutes).seconds),
        )
        cleanUpCache(now)
    }

    private fun cleanUpCache(now: Instant) {
        cache.entries.removeIf { (_, entry) -> entry.expireAt.isBefore(now) }
        val maxEntries = props.cacheMaxEntries.coerceAtLeast(50)
        if (cache.size <= maxEntries) return
        val overflow = cache.size - maxEntries
        cache.entries.asSequence()
            .sortedBy { (_, entry) -> entry.cachedAt }
            .take(overflow)
            .forEach { (key, _) -> cache.remove(key) }
    }

    private data class RawTourApiResponse(val statusCode: HttpStatusCode, val body: String)
    private data class TourApiCacheKey(val baseYm: String, val areaCode: Int, val signguCode: Int, val size: Int)
    private data class TourApiCacheEntry(val attractions: List<TourAttraction>, val cachedAt: Instant, val expireAt: Instant)
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TourApiResponseEnvelope(val response: TourApiResponse? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TourApiResponse(
    val header: TourApiHeader = TourApiHeader(),
    val body: TourApiBody = TourApiBody(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TourApiHeader(
    val resultCode: String? = null,
    val resultMsg: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TourApiBody(val items: TourApiItems = TourApiItems())

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TourApiItems(
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    val item: List<TourApiItem> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class TourApiItem(
    val baseYm: String? = null,
    val mapX: String? = null,
    val mapY: String? = null,
    val areaCd: String? = null,
    val areaNm: String? = null,
    val signguCd: String? = null,
    val signguNm: String? = null,
    val hubTatsCd: String? = null,
    val hubTatsNm: String? = null,
    val hubCtgryLclsNm: String? = null,
    val hubCtgryMclsNm: String? = null,
    val hubRank: String? = null,
)
