package com.sleekydz86.idolglow.exchangerate.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sleekydz86.idolglow.exchangerate.application.port.out.ExchangeRateQueryPort
import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import com.sleekydz86.idolglow.global.infrastructure.config.KoreaEximExchangeProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.core.ParameterizedTypeReference
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
class KoreaEximExchangeRateClient(
    private val webClient: WebClient,
    private val properties: KoreaEximExchangeProperties,
) : ExchangeRateQueryPort {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun fetchDailyRates(searchDate: LocalDate?): List<ExchangeRateQuote> {
        val key = properties.authKey.trim()
        if (key.isEmpty()) {
            log.warn("app.exchange.korea-exim.auth-key가 비어 있어 빈 목록을 반환합니다.")
            return emptyList()
        }
        val base = properties.baseUrl.trim().trimEnd('/')
        val urlBase = "$base/exchangeJSON"
        val builder = UriComponentsBuilder.fromUriString(urlBase)
            .queryParam("authkey", key)
            .queryParam("data", "AP01")
        if (searchDate != null) {
            builder.queryParam("searchdate", searchDate.format(DATE_PARAM))
        }
        val requestUri: URI = builder.build().encode().toUri()

        val raw: List<KoreaEximRowJson> = try {
            webClient.get()
                .uri(requestUri)
                .retrieve()
                .bodyToMono(object : ParameterizedTypeReference<List<KoreaEximRowJson>>() {})
                .block()
        } catch (e: Exception) {
            log.warn("한국수출입은행 환율 API 호출 실패: {}", e.message)
            return emptyList()
        } ?: emptyList()

        if (raw.isNotEmpty() && raw.first().result != null && raw.first().result != 1) {
            val code = raw.first().result
            log.warn("한국수출입은행 환율 API 응답 result={} (1=정상, 2=DATA, 3=인증, 4=일일한도). 적용할 행이 없습니다.", code)
            return emptyList()
        }

        return raw.mapNotNull { row -> toDomain(row) }
    }

    private fun toDomain(row: KoreaEximRowJson): ExchangeRateQuote? {
        if (row.result != null && row.result != 1) {
            return null
        }
        val unit = row.curUnit?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val deal = row.dealBasR?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        return ExchangeRateQuote(
            curUnit = unit,
            curNm = row.curNm?.trim().orEmpty(),
            ttb = row.ttb?.trim().orEmpty(),
            tts = row.tts?.trim().orEmpty(),
            dealBasR = deal,
            bkpr = row.bkpr?.trim().orEmpty(),
        )
    }

    companion object {
        private val DATE_PARAM: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class KoreaEximRowJson(
    @JsonProperty("result") val result: Int? = 1,
    @JsonProperty("cur_unit") val curUnit: String? = null,
    @JsonProperty("cur_nm") val curNm: String? = null,
    @JsonProperty("ttb") val ttb: String? = null,
    @JsonProperty("tts") val tts: String? = null,
    @JsonProperty("deal_bas_r") val dealBasR: String? = null,
    @JsonProperty("bkpr") val bkpr: String? = null,
)
