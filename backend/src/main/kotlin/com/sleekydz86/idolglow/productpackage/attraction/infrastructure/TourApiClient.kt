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
import org.springframework.web.util.UriUtils
import org.springframework.web.reactive.function.client.WebClient
import java.net.URI
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

        val encodedServiceKey = resolveEncodedServiceKey(props.serviceKey)
        if (encodedServiceKey.isEmpty()) {
            throw CustomException(TourAttractionExceptionType.TOUR_API_KEY_MISSING)
        }

        val rawResponse = requestWithFallback(
            encodedServiceKey = encodedServiceKey,
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
        encodedServiceKey: String,
        baseYm: String,
        areaCode: Int,
        signguCode: Int,
        requestedSize: Int,
        key: TourApiCacheKey,
    ): RawTourApiResponse {
        val primarySize = requestedSize.coerceIn(1, 1000)
        val sizes = if (primarySize > 100) listOf(primarySize, 100) else listOf(primarySize)

        var lastFailedResponse: RawTourApiResponse? = null
        for (size in sizes) {
            val response = requestRawResponse(
                encodedServiceKey = encodedServiceKey,
                baseYm = baseYm,
                areaCode = areaCode,
                signguCode = signguCode,
                numOfRows = size,
            )
            if (response.statusCode.is2xxSuccessful) {
                if (size != primarySize) {
                    log.info("Tour API 재시도 성공. numOfRows={} -> {}", primarySize, size)
                }
                return response
            }
            lastFailedResponse = response
            log.warn(
                "Tour API 비정상 응답. status={}, numOfRows={}, body={}",
                response.statusCode.value(),
                size,
                response.body.take(300),
            )
        }

        if (lastFailedResponse != null) {
            log.warn(
                "Tour API 최종 실패. status={}, body={}",
                lastFailedResponse.statusCode.value(),
                lastFailedResponse.body.take(300),
            )
        }
        staleOrThrow(key, TourAttractionExceptionType.TOUR_API_CALL_FAILED)
        throw CustomException(TourAttractionExceptionType.TOUR_API_CALL_FAILED)
    }

    private fun requestRawResponse(
        encodedServiceKey: String,
        baseYm: String,
        areaCode: Int,
        signguCode: Int,
        numOfRows: Int,
    ): RawTourApiResponse {
        return try {
            val requestUri = URI.create(
                buildRequestUri(
                    encodedServiceKey = encodedServiceKey,
                    baseYm = baseYm,
                    areaCode = areaCode,
                    signguCode = signguCode,
                    numOfRows = numOfRows,
                ),
            )
            webClient.get()
                .uri(requestUri)
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

    private fun buildRequestUri(
        encodedServiceKey: String,
        baseYm: String,
        areaCode: Int,
        signguCode: Int,
        numOfRows: Int,
    ): String {
        val encodedMobileOs = UriUtils.encodeQueryParam(props.mobileOs, StandardCharsets.UTF_8)
        val encodedMobileApp = UriUtils.encodeQueryParam(props.mobileApp, StandardCharsets.UTF_8)
        return "${endpointUrl()}?serviceKey=$encodedServiceKey" +
            "&pageNo=1" +
            "&numOfRows=$numOfRows" +
            "&MobileOS=$encodedMobileOs" +
            "&MobileApp=$encodedMobileApp" +
            "&baseYm=$baseYm" +
            "&areaCd=$areaCode" +
            "&signguCd=$signguCode" +
            "&_type=json"
    }

    private fun resolveEncodedServiceKey(rawServiceKey: String): String {
        val trimmed = rawServiceKey.trim().removePrefix("TOUR_API_SERVICE_KEY=").removePrefix("serviceKey=")
        if (trimmed.isEmpty()) return ""
        return if (trimmed.contains('%')) trimmed else UriUtils.encodeQueryParam(trimmed, StandardCharsets.UTF_8)
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
