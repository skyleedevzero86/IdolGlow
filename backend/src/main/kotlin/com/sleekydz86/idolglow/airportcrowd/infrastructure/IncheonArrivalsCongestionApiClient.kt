package com.sleekydz86.idolglow.airportcrowd.infrastructure

import com.sleekydz86.idolglow.airportcrowd.application.port.out.ArrivalCongestionQueryPort
import com.sleekydz86.idolglow.airportcrowd.domain.ArrivalCongestion
import com.sleekydz86.idolglow.global.infrastructure.config.IncheonAirportArrivalsCongestionProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriUtils
import org.w3c.dom.Element
import org.xml.sax.InputSource
import java.io.StringReader
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import javax.xml.parsers.DocumentBuilderFactory

@Component
class IncheonArrivalsCongestionApiClient(
    private val webClient: WebClient,
    private val properties: IncheonAirportArrivalsCongestionProperties,
) : ArrivalCongestionQueryPort {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun fetchCurrent(
        terminal: String?,
        airport: String?,
        pageNo: Int,
        numOfRows: Int,
    ): List<ArrivalCongestion> {
        val encodedKey = resolveEncodedServiceKey(properties.serviceKey)
        if (encodedKey.isBlank()) return emptyList()
        val url = buildUrl(
            encodedServiceKey = encodedKey,
            terminal = terminal,
            airport = airport,
            pageNo = pageNo.coerceAtLeast(1),
            numOfRows = numOfRows.coerceIn(1, 1000),
        )
        val response = requestRaw(url)
        if (!response.statusCode.is2xxSuccessful) {
            log.warn("입국장 혼잡도 HTTP 오류. status={}, body={}", response.statusCode.value(), response.body.take(300))
            return emptyList()
        }
        if (response.body.contains("<OpenAPI_ServiceResponse>", ignoreCase = true)) {
            log.warn("입국장 혼잡도 공공데이터 에러 XML 응답. body={}", response.body.take(300))
            return emptyList()
        }
        return parseXml(response.body)
    }

    private fun parseXml(xml: String): List<ArrivalCongestion> {
        return runCatching {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
            val header = doc.getElementsByTagName("header").item(0) as? Element
            val resultCode = header?.childText("resultCode").orEmpty()
            if (resultCode.isNotBlank() && resultCode != "00") {
                log.warn(
                    "입국장 혼잡도 기관 오류. resultCode={}, resultMsg={}",
                    resultCode,
                    header?.childText("resultMsg").orEmpty(),
                )
                return emptyList()
            }
            val itemNodes = doc.getElementsByTagName("item")
            (0 until itemNodes.length).mapNotNull { idx ->
                val item = itemNodes.item(idx) as? Element ?: return@mapNotNull null
                val terminal = item.childText("terno")?.trim()?.uppercase().orEmpty()
                if (terminal.isBlank()) return@mapNotNull null
                ArrivalCongestion(
                    terminal = terminal,
                    airport = item.childText("airport"),
                    entryGate = item.childText("entrygate"),
                    gateNumber = item.childText("gatenumber"),
                    flightId = item.childText("flightid"),
                    korean = item.childText("korean").toIntFromDecimal(),
                    foreigner = item.childText("foreigner").toIntFromDecimal(),
                    scheduleTime = parseDateTime(item.childText("scheduletime")),
                    estimatedTime = parseDateTime(item.childText("estimatedtime")),
                )
            }
        }.getOrElse {
            log.warn("입국장 혼잡도 XML 파싱 실패: {}", it.message)
            emptyList()
        }
    }

    private fun buildUrl(
        encodedServiceKey: String,
        terminal: String?,
        airport: String?,
        pageNo: Int,
        numOfRows: Int,
    ): String {
        val query = buildList {
            add("serviceKey=$encodedServiceKey")
            add("type=xml")
            add("pageNo=$pageNo")
            add("numOfRows=$numOfRows")
            terminal?.takeIf { it.isNotBlank() }?.let {
                add("terno=${UriUtils.encodeQueryParam(it, StandardCharsets.UTF_8)}")
            }
            airport?.takeIf { it.isNotBlank() }?.let {
                add("airport=${UriUtils.encodeQueryParam(it, StandardCharsets.UTF_8)}")
            }
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
            log.warn("입국장 혼잡도 API 호출 실패: {}", e.message)
            RawHttpResponse(HttpStatusCode.valueOf(502), e.message ?: "")
        }
    }

    private fun resolveEncodedServiceKey(rawServiceKey: String): String {
        val trimmed = rawServiceKey.trim().removePrefix("serviceKey=").removePrefix("AIRPORT_ARRIVALS_CONGESTION_SERVICE_KEY=")
        if (trimmed.isBlank()) return ""
        return if (trimmed.contains('%')) trimmed else UriUtils.encodeQueryParam(trimmed, StandardCharsets.UTF_8)
    }

    private fun parseDateTime(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        return runCatching { LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER) }.getOrNull()
    }

    private fun String?.toIntFromDecimal(): Int? {
        if (this.isNullOrBlank()) return null
        return this.trim().toDoubleOrNull()?.toInt()
    }

    private fun Element.childText(tag: String): String? {
        val node = this.getElementsByTagName(tag).item(0) as? Element ?: return null
        return node.textContent?.trim()?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private val DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmm")
    }
}
