package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sleekydz86.idolglow.eventinfo.domain.FestivalEvent
import com.sleekydz86.idolglow.global.infrastructure.config.SeoulSjwApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class SeoulSjwPerformApiClient(
    private val webClient: WebClient,
    private val seoulSjwApiProperties: SeoulSjwApiProperties,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun list(pageNo: Int, numOfRows: Int): List<FestivalEvent> {
        val key = seoulSjwApiProperties.apiKey.trim()
        if (key.isEmpty()) return emptyList()
        val start = ((pageNo.coerceAtLeast(1) - 1) * numOfRows.coerceIn(1, 1000)) + 1
        val end = start + numOfRows.coerceIn(1, 1000) - 1
        val path = "/$key/json/SJWPerform/$start/$end"
        val response = runCatching {
            webClient.get().uri("${seoulSjwApiProperties.baseUrl.trimEnd('/')}$path")
                .retrieve()
                .bodyToMono(SjwEnvelope::class.java)
                .block()
        }.getOrElse {
            log.warn("SJWPerform 목록 호출 실패: {}", it.message)
            null
        } ?: return emptyList()
        val resultCode = response.sjwPerform?.result?.code
        if (resultCode != null && resultCode != "INFO-000" && resultCode != "INFO-200") {
            log.warn("SJWPerform 제공기관 오류. code={}, message={}", resultCode, response.sjwPerform?.result?.message)
            return emptyList()
        }
        return response.sjwPerform?.rows.orEmpty().mapNotNull { it.toDomain() }
    }

    fun detail(performIdx: String): FestivalEvent? {
        val key = seoulSjwApiProperties.apiKey.trim()
        if (key.isEmpty()) return null
        val path = "/$key/json/SJWPerform/1/1/$performIdx"
        val response = runCatching {
            webClient.get().uri("${seoulSjwApiProperties.baseUrl.trimEnd('/')}$path")
                .retrieve()
                .bodyToMono(SjwEnvelope::class.java)
                .block()
        }.getOrElse {
            log.warn("SJWPerform 상세 호출 실패: {}", it.message)
            null
        } ?: return null
        val resultCode = response.sjwPerform?.result?.code
        if (resultCode != null && resultCode != "INFO-000") {
            return null
        }
        return response.sjwPerform?.rows.orEmpty().firstOrNull()?.toDomain()
    }

    fun isEventActiveOnDate(event: FestivalEvent, yyyymmdd: String): Boolean {
        val date = parseDate(yyyymmdd) ?: return true
        val start = event.eventStartDate?.let(::parseDate)
        val end = event.eventEndDate?.let(::parseDate)
        return when {
            start != null && end != null -> !date.isBefore(start) && !date.isAfter(end)
            start != null -> !date.isBefore(start)
            end != null -> !date.isAfter(end)
            else -> true
        }
    }

    private fun parseDate(value: String): LocalDate? =
        runCatching { LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE) }.getOrNull()
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SjwEnvelope(
    @JsonProperty("SJWPerform")
    val sjwPerform: SjwBody? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SjwBody(
    @JsonProperty("RESULT")
    val result: SjwResult? = null,
    @JsonProperty("row")
    val rows: List<SjwRow> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SjwResult(
    @JsonProperty("CODE")
    val code: String? = null,
    @JsonProperty("MESSAGE")
    val message: String? = null,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SjwRow(
    @JsonProperty("PERFORM_IDX")
    val performIdx: String? = null,
    @JsonProperty("TITLE")
    val title: String? = null,
    @JsonProperty("START_DATE")
    val startDate: String? = null,
    @JsonProperty("END_DATE")
    val endDate: String? = null,
    @JsonProperty("PLACE_LIST")
    val placeList: String? = null,
    @JsonProperty("FILE_URL")
    val fileUrl: String? = null,
    @JsonProperty("INFO_URL")
    val infoUrl: String? = null,
    @JsonProperty("GENRE_NAME")
    val genreName: String? = null,
    @JsonProperty("SYNOPSIS")
    val synopsis: String? = null,
    @JsonProperty("INQUIRY_PHONE")
    val inquiryPhone: String? = null,
) {
    fun toDomain(): FestivalEvent? {
        val id = performIdx?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val t = title?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return FestivalEvent(
            contentId = id,
            title = t,
            address = placeList?.trim()?.takeIf { it.isNotEmpty() },
            eventStartDate = startDate?.trim()?.takeIf { it.isNotEmpty() },
            eventEndDate = endDate?.trim()?.takeIf { it.isNotEmpty() },
            thumbnailImageUrl = fileUrl?.trim()?.takeIf { it.isNotEmpty() },
            imageUrl = fileUrl?.trim()?.takeIf { it.isNotEmpty() },
            mapX = null,
            mapY = null,
            phone = inquiryPhone?.trim()?.takeIf { it.isNotEmpty() },
            detailUrl = infoUrl?.trim()?.takeIf { it.isNotEmpty() },
            category = genreName?.trim()?.takeIf { it.isNotEmpty() },
            synopsis = synopsis?.trim()?.takeIf { it.isNotEmpty() },
            source = "SEOUL_SJW_PERFORM",
        )
    }
}
