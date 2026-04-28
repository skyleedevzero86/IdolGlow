package com.sleekydz86.idolglow.airportcrowd.infrastructure

import com.sleekydz86.idolglow.airportcrowd.application.port.out.PassengerForecastQueryPort
import com.sleekydz86.idolglow.airportcrowd.domain.PassengerForecast
import com.sleekydz86.idolglow.global.infrastructure.config.IncheonAirportPassengerForecastProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriUtils
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

@Component
class IncheonPassengerForecastApiClient(
    private val webClient: WebClient,
    private val properties: IncheonAirportPassengerForecastProperties,
) : PassengerForecastQueryPort {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun fetch(selectDate: Int, pageNo: Int, numOfRows: Int): List<PassengerForecast> {
        require(selectDate == 0 || selectDate == 1) { "selectDate는 0 또는 1이어야 합니다." }
        val encodedKey = resolveEncodedServiceKey(properties.serviceKey)
        if (encodedKey.isBlank()) return emptyList()
        val uri = buildUrl(
            encodedServiceKey = encodedKey,
            selectDate = selectDate,
            pageNo = pageNo.coerceAtLeast(1),
            numOfRows = numOfRows.coerceIn(1, 10000),
        )
        val response = requestRaw(uri)
        if (!response.statusCode.is2xxSuccessful) {
            log.warn("출입국 승객 예고 HTTP 오류. status={}, body={}", response.statusCode.value(), response.body.take(300))
            return emptyList()
        }
        if (response.body.contains("<OpenAPI_ServiceResponse>", ignoreCase = true)) {
            log.warn("출입국 승객 예고 공공데이터 에러 XML 응답. body={}", response.body.take(300))
            return emptyList()
        }
        return parseXml(response.body)
    }

    private fun parseXml(xml: String): List<PassengerForecast> {
        return runCatching {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
            val header = doc.getElementsByTagName("header").item(0) as? Element
            val resultCode = header?.let { childText(it, "resultCode") }.orEmpty()
            if (resultCode.isNotBlank() && resultCode != "00") {
                log.warn(
                    "출입국 승객 예고 기관 오류. resultCode={}, resultMsg={}",
                    resultCode,
                    header?.let { childText(it, "resultMsg") }.orEmpty(),
                )
                return emptyList()
            }
            val itemNodes = doc.getElementsByTagName("item")
            (0 until itemNodes.length).mapNotNull { idx ->
                val item = itemNodes.item(idx) as? Element ?: return@mapNotNull null
                PassengerForecast(
                    date = childText(item, "adate")
                        ?.let { runCatching { LocalDate.parse(it, DATE_FORMATTER) }.getOrNull() },
                    timeSlot = childText(item, "atime") ?: return@mapNotNull null,
                    terminal1DepartureTotal = childText(item, "t1dgsum1").toIntFromDecimal(),
                    terminal2DepartureTotal = childText(item, "t2dgsum2").toIntFromDecimal(),
                    terminal1ArrivalTotal = childText(item, "t1egsum1").toIntFromDecimal(),
                    terminal2ArrivalTotal = childText(item, "t2egsum1").toIntFromDecimal(),
                )
            }
        }.getOrElse {
            log.warn("출입국 승객 예고 XML 파싱 실패: {}", it.message)
            emptyList()
        }
    }

    private fun buildUrl(
        encodedServiceKey: String,
        selectDate: Int,
        pageNo: Int,
        numOfRows: Int,
    ): String {
        val query = buildList {
            add("serviceKey=$encodedServiceKey")
            add("selectdate=$selectDate")
            add("type=xml")
            add("pageNo=$pageNo")
            add("numOfRows=$numOfRows")
        }.joinToString("&")
        return "${properties.baseUrl.trimEnd('/')}?$query"
    }

    private fun requestRaw(url: String): RawHttpResponse {
        return runCatching {
            webClient.get()
                .uri(url)
                .exchangeToMono { response ->
                    response.bodyToMono(String::class.java)
                        .defaultIfEmpty("")
                        .map { body -> RawHttpResponse(response.statusCode(), body) }
                }
                .block()
                ?: RawHttpResponse(HttpStatusCode.valueOf(502), "")
        }.getOrElse { e ->
            log.warn("출입국 승객 예고 API 호출 실패: {}", e.message)
            RawHttpResponse(HttpStatusCode.valueOf(502), e.message ?: "")
        }
    }

    private fun resolveEncodedServiceKey(rawServiceKey: String): String {
        val trimmed = rawServiceKey.trim().removePrefix("serviceKey=").removePrefix("AIRPORT_PASSENGER_FORECAST_SERVICE_KEY=")
        if (trimmed.isBlank()) return ""
        return if (trimmed.contains('%')) trimmed else UriUtils.encodeQueryParam(trimmed, StandardCharsets.UTF_8)
    }

    private fun childText(parent: Element, tag: String): String? {
        val node = parent.getElementsByTagName(tag).item(0) as? Element ?: return null
        return node.textContent?.trim()?.takeIf { it.isNotEmpty() }
    }

    private fun String?.toIntFromDecimal(): Int? {
        if (this.isNullOrBlank()) return null
        return this.trim().toDoubleOrNull()?.toInt()
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.BASIC_ISO_DATE
    }
}
