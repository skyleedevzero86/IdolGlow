package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.fasterxml.jackson.annotation.JsonFormat
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.sleekydz86.idolglow.eventinfo.application.port.out.FestivalEventExternalQueryPort
import com.sleekydz86.idolglow.eventinfo.domain.CodeEntry
import com.sleekydz86.idolglow.eventinfo.domain.FestivalCommonDetail
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import com.sleekydz86.idolglow.eventinfo.domain.FestivalImage
import com.sleekydz86.idolglow.global.infrastructure.config.TourKorApiProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriUtils
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.nio.charset.StandardCharsets

@Component
class TourFestivalApiClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val tourKorApiProperties: TourKorApiProperties,
) : FestivalEventExternalQueryPort {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun searchFestival(
        eventStartDate: String,
        eventEndDate: String,
        pageNo: Int,
        numOfRows: Int,
    ): List<FestivalEvent> {
        val encodedKey = resolveEncodedServiceKey(tourKorApiProperties.serviceKey)
        if (encodedKey.isEmpty()) {
            log.warn("Tour Kor API serviceKey가 비어 있습니다.")
            return emptyList()
        }
        return fetchList<FestivalItem>(
            endpoint = "searchFestival2",
            encodedServiceKey = encodedKey,
            params = mapOf(
                "arrange" to "Q",
                "eventStartDate" to eventStartDate,
                "eventEndDate" to eventEndDate,
                "pageNo" to pageNo.toString(),
                "numOfRows" to numOfRows.toString(),
            ),
        ).mapNotNull { it.toDomain() }
    }

    override fun detailCommon(contentId: String): FestivalCommonDetail? {
        val encodedKey = resolveEncodedServiceKey(tourKorApiProperties.serviceKey)
        if (encodedKey.isEmpty()) return null
        return fetchList<DetailCommonItem>(
            endpoint = "detailCommon2",
            encodedServiceKey = encodedKey,
            params = mapOf(
                "contentId" to contentId,
                "pageNo" to "1",
                "numOfRows" to "10",
            ),
        ).firstOrNull()?.toDomain()
    }

    override fun detailImage(contentId: String, imageYn: String): List<FestivalImage> {
        val encodedKey = resolveEncodedServiceKey(tourKorApiProperties.serviceKey)
        if (encodedKey.isEmpty()) return emptyList()
        return fetchList<DetailImageItem>(
            endpoint = "detailImage2",
            encodedServiceKey = encodedKey,
            params = mapOf(
                "contentId" to contentId,
                "imageYN" to imageYn,
                "pageNo" to "1",
                "numOfRows" to "30",
            ),
        ).mapNotNull { it.toDomain() }
    }

    override fun searchKeyword(
        keyword: String,
        pageNo: Int,
        numOfRows: Int,
        lDongRegnCd: String?,
        lDongSignguCd: String?,
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
    ): List<FestivalEvent> {
        val encodedKey = resolveEncodedServiceKey(tourKorApiProperties.serviceKey)
        if (encodedKey.isEmpty()) return emptyList()
        return fetchList<FestivalItem>(
            endpoint = "searchKeyword2",
            encodedServiceKey = encodedKey,
            params = buildMap {
                put("arrange", "Q")
                put("keyword", keyword)
                put("contentTypeId", "85")
                put("pageNo", pageNo.toString())
                put("numOfRows", numOfRows.toString())
                lDongRegnCd?.let { put("lDongRegnCd", it) }
                lDongSignguCd?.let { put("lDongSignguCd", it) }
                lclsSystm1?.let { put("lclsSystm1", it) }
                lclsSystm2?.let { put("lclsSystm2", it) }
                lclsSystm3?.let { put("lclsSystm3", it) }
            },
        ).mapNotNull { it.toDomain() }
    }

    override fun lDongCodes(lDongRegnCd: String?, lDongListYn: String): List<CodeEntry> {
        val encodedKey = resolveEncodedServiceKey(tourKorApiProperties.serviceKey)
        if (encodedKey.isEmpty()) return emptyList()
        return fetchList<CodeItem>(
            endpoint = "ldongCode2",
            encodedServiceKey = encodedKey,
            params = buildMap {
                put("lDongListYn", lDongListYn)
                put("pageNo", "1")
                put("numOfRows", "500")
                lDongRegnCd?.let { put("lDongRegnCd", it) }
            },
        ).map { it.toDomain() }
    }

    override fun lclsCodes(
        lclsSystm1: String?,
        lclsSystm2: String?,
        lclsSystm3: String?,
        lclsSystmListYn: String,
    ): List<CodeEntry> {
        val encodedKey = resolveEncodedServiceKey(tourKorApiProperties.serviceKey)
        if (encodedKey.isEmpty()) return emptyList()
        return fetchList<CodeItem>(
            endpoint = "lclsSystmCode2",
            encodedServiceKey = encodedKey,
            params = buildMap {
                put("lclsSystmListYn", lclsSystmListYn)
                put("pageNo", "1")
                put("numOfRows", "500")
                lclsSystm1?.let { put("lclsSystm1", it) }
                lclsSystm2?.let { put("lclsSystm2", it) }
                lclsSystm3?.let { put("lclsSystm3", it) }
            },
        ).map { it.toDomain() }
    }

    private inline fun <reified T> fetchList(
        endpoint: String,
        encodedServiceKey: String,
        params: Map<String, String>,
    ): List<T> {
        val response = requestRaw(endpoint, encodedServiceKey, params)
        if (!response.statusCode.is2xxSuccessful) {
            log.warn("Tour API HTTP 오류. endpoint={}, status={}, body={}", endpoint, response.statusCode.value(), response.body.take(300))
            return emptyList()
        }
        if (response.body.contains("<OpenAPI_ServiceResponse>", ignoreCase = true)) {
            log.warn("Tour API XML 오류 응답. endpoint={}, body={}", endpoint, response.body.take(300))
            return emptyList()
        }
        val parsed = runCatching {
            objectMapper.readValue(response.body, FestivalResponseEnvelope::class.java)
        }.getOrElse { e ->
            log.warn("Tour API 파싱 실패. endpoint={}, message={}", endpoint, e.message)
            return emptyList()
        }
        if (parsed.response?.header?.resultCode != "0000") {
            log.warn(
                "Tour API 제공기관 오류. endpoint={}, resultCode={}, resultMsg={}",
                endpoint,
                parsed.response?.header?.resultCode,
                parsed.response?.header?.resultMsg,
            )
            return emptyList()
        }
        return parsed.response?.body?.items?.item.orEmpty().mapNotNull {
            runCatching { objectMapper.convertValue(it, T::class.java) }.getOrNull()
        }
    }

    private fun requestRaw(
        endpoint: String,
        encodedServiceKey: String,
        params: Map<String, String>,
    ): RawFestivalResponse {
        val requestUri = URI.create(
            buildUri(endpoint, encodedServiceKey, params),
        )
        return runCatching {
            webClient.get()
                .uri(requestUri)
                .exchangeToMono { clientResponse ->
                    clientResponse.bodyToMono(String::class.java)
                        .defaultIfEmpty("")
                        .map { body -> RawFestivalResponse(clientResponse.statusCode(), body) }
                }
                .block()
                ?: RawFestivalResponse(HttpStatusCode.valueOf(502), "")
        }.getOrElse { e ->
            log.warn("Tour festival API 호출 실패: {}", e.message)
            RawFestivalResponse(HttpStatusCode.valueOf(502), e.message ?: "")
        }
    }

    private fun buildUri(endpoint: String, encodedServiceKey: String, params: Map<String, String>): String {
        val encodedMobileOs = UriUtils.encodeQueryParam(tourKorApiProperties.mobileOs, StandardCharsets.UTF_8)
        val encodedMobileApp = UriUtils.encodeQueryParam(tourKorApiProperties.mobileApp, StandardCharsets.UTF_8)
        val query = params.entries.joinToString("&") { (k, v) ->
            "$k=${UriUtils.encodeQueryParam(v, StandardCharsets.UTF_8)}"
        }
        return "${tourKorApiProperties.baseUrl.trimEnd('/')}/$endpoint" +
            "?serviceKey=$encodedServiceKey" +
            "&MobileOS=$encodedMobileOs" +
            "&MobileApp=$encodedMobileApp" +
            "&_type=json" +
            if (query.isBlank()) "" else "&$query"
    }

    private fun resolveEncodedServiceKey(rawServiceKey: String): String {
        val trimmed = rawServiceKey.trim().removePrefix("TOUR_API_SERVICE_KEY=").removePrefix("serviceKey=")
        if (trimmed.isEmpty()) return ""
        return if (trimmed.contains('%')) trimmed else UriUtils.encodeQueryParam(trimmed, StandardCharsets.UTF_8)
    }
}

private data class RawFestivalResponse(val statusCode: HttpStatusCode, val body: String)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class FestivalResponseEnvelope(val response: FestivalResponse? = null)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class FestivalResponse(
    val header: FestivalHeader = FestivalHeader(),
    val body: FestivalBody = FestivalBody(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class FestivalHeader(
    val resultCode: String? = null,
    val resultMsg: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class FestivalBody(
    @JsonDeserialize(using = LenientFestivalItemsDeserializer::class)
    val items: FestivalItems = FestivalItems(),
)

private class LenientFestivalItemsDeserializer : JsonDeserializer<FestivalItems>() {
    override fun deserialize(p: JsonParser, ctxt: DeserializationContext): FestivalItems =
        when (p.currentToken()) {
            JsonToken.VALUE_STRING, JsonToken.VALUE_NULL -> FestivalItems()
            JsonToken.START_OBJECT -> ctxt.readValue(p, FestivalItems::class.java)
            else -> FestivalItems()
        }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class FestivalItems(
    @JsonFormat(with = [JsonFormat.Feature.ACCEPT_SINGLE_VALUE_AS_ARRAY])
    val item: List<Map<String, Any?>> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class FestivalItem(
    val contentid: String? = null,
    val title: String? = null,
    val addr1: String? = null,
    val eventstartdate: String? = null,
    val eventenddate: String? = null,
    val firstimage2: String? = null,
    val firstimage: String? = null,
    val mapx: String? = null,
    val mapy: String? = null,
    val tel: String? = null,
) {
    fun toDomain(): FestivalEvent? {
        val id = contentid?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val t = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return FestivalEvent(
            contentId = id,
            title = t,
            address = addr1?.trim()?.takeIf { it.isNotEmpty() },
            eventStartDate = eventstartdate?.trim()?.takeIf { it.isNotEmpty() },
            eventEndDate = eventenddate?.trim()?.takeIf { it.isNotEmpty() },
            thumbnailImageUrl = firstimage2?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = firstimage?.trim()?.takeIf { it.isNotEmpty() },
            mapX = mapx?.toDoubleOrNull(),
            mapY = mapy?.toDoubleOrNull(),
            phone = tel?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class DetailCommonItem(
    val contentid: String? = null,
    val contenttypeid: String? = null,
    val title: String? = null,
    val homepage: String? = null,
    val overview: String? = null,
    val addr1: String? = null,
    val addr2: String? = null,
    val mapx: String? = null,
    val mapy: String? = null,
    val tel: String? = null,
    val firstimage: String? = null,
    val firstimage2: String? = null,
) {
    fun toDomain(): FestivalCommonDetail? {
        val id = contentid?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return FestivalCommonDetail(
            contentId = id,
            contentTypeId = contenttypeid?.trim()?.takeIf { it.isNotEmpty() },
            title = title?.trim()?.takeIf { it.isNotEmpty() },
            homepage = homepage?.trim()?.takeIf { it.isNotEmpty() },
            overview = overview?.trim()?.takeIf { it.isNotEmpty() },
            address = addr1?.trim()?.takeIf { it.isNotEmpty() },
            addressDetail = addr2?.trim()?.takeIf { it.isNotEmpty() },
            mapX = mapx?.toDoubleOrNull(),
            mapY = mapy?.toDoubleOrNull(),
            tel = tel?.trim()?.takeIf { it.isNotEmpty() },
            firstImage = firstimage?.trim()?.takeIf { it.isNotEmpty() },
            firstImage2 = firstimage2?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class DetailImageItem(
    val contentid: String? = null,
    val imgname: String? = null,
    val originimgurl: String? = null,
    val smallimageurl: String? = null,
    val cpyrhtDivCd: String? = null,
    val serialnum: String? = null,
) {
    fun toDomain(): FestivalImage? {
        val id = contentid?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return FestivalImage(
            contentId = id,
            imageName = imgname?.trim()?.takeIf { it.isNotEmpty() },
            originImageUrl = originimgurl?.trim()?.takeIf { it.isNotEmpty() },
            smallImageUrl = smallimageurl?.trim()?.takeIf { it.isNotEmpty() },
            copyrightType = cpyrhtDivCd?.trim()?.takeIf { it.isNotEmpty() },
            serialNum = serialnum?.trim()?.takeIf { it.isNotEmpty() },
        )
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class CodeItem(
    val code: String? = null,
    val name: String? = null,
    val rnum: Int? = null,
    val lDongRegnCd: String? = null,
    val lDongRegnNm: String? = null,
    val lDongSignguCd: String? = null,
    val lDongSignguNm: String? = null,
    val lclsSystm1Cd: String? = null,
    val lclsSystm1Nm: String? = null,
    val lclsSystm2Cd: String? = null,
    val lclsSystm2Nm: String? = null,
    val lclsSystm3Cd: String? = null,
    val lclsSystm3Nm: String? = null,
) {
    fun toDomain(): CodeEntry {
        val noLcls =
            lclsSystm1Cd.isNullOrBlank() &&
                lclsSystm2Cd.isNullOrBlank() &&
                lclsSystm3Cd.isNullOrBlank()
        val rawRegn = lDongRegnCd?.trim()?.takeIf { it.isNotEmpty() }
        val rawSigngu = lDongSignguCd?.trim()?.takeIf { it.isNotEmpty() }
        val trimmedCode = code?.trim()?.takeIf { it.isNotEmpty() }
        val regnFromCode =
            rawRegn == null &&
                rawSigngu == null &&
                noLcls &&
                trimmedCode != null
        val effectiveRegnCd = rawRegn ?: if (regnFromCode) trimmedCode else null
        val effectiveRegnNm =
            lDongRegnNm?.trim()?.takeIf { it.isNotEmpty() }
                ?: if (regnFromCode) name?.trim()?.takeIf { it.isNotEmpty() } else null
        val effectiveSignguCd =
            rawSigngu
                ?: if (
                    noLcls &&
                        effectiveRegnCd != null &&
                        trimmedCode != null &&
                        trimmedCode != effectiveRegnCd
                ) {
                    trimmedCode
                } else {
                    null
                }
        val effectiveSignguNm =
            lDongSignguNm?.trim()?.takeIf { it.isNotEmpty() }
                ?: if (effectiveSignguCd != null && rawSigngu == null) name?.trim()?.takeIf { it.isNotEmpty() } else null
        return CodeEntry(
            code = code,
            name = name,
            rnum = rnum,
            lDongRegnCd = effectiveRegnCd,
            lDongRegnNm = effectiveRegnNm,
            lDongSignguCd = effectiveSignguCd,
            lDongSignguNm = effectiveSignguNm,
            lclsSystm1Cd = lclsSystm1Cd,
            lclsSystm1Nm = lclsSystm1Nm,
            lclsSystm2Cd = lclsSystm2Cd,
            lclsSystm2Nm = lclsSystm2Nm,
            lclsSystm3Cd = lclsSystm3Cd,
            lclsSystm3Nm = lclsSystm3Nm,
        )
    }
}
