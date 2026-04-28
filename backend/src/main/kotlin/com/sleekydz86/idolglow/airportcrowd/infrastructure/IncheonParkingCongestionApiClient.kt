package com.sleekydz86.idolglow.airportcrowd.infrastructure

import com.sleekydz86.idolglow.airportcrowd.application.port.out.ParkingCongestionQueryPort
import com.sleekydz86.idolglow.airportcrowd.domain.ParkingCongestion
import com.sleekydz86.idolglow.global.infrastructure.config.IncheonAirportParkingProperties
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
class IncheonParkingCongestionApiClient(
    private val webClient: WebClient,
    private val properties: IncheonAirportParkingProperties,
    private val authCooldown: IncheonAirportApiAuthCooldown,
) : ParkingCongestionQueryPort {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun fetchCurrent(pageNo: Int, numOfRows: Int): List<ParkingCongestion> {
        if (authCooldown.isBlocked(API_NAME)) return emptyList()
        val encodedKey = resolveEncodedServiceKey(properties.serviceKey)
        if (encodedKey.isBlank()) return emptyList()

        val url = buildUrl(
            encodedServiceKey = encodedKey,
            pageNo = pageNo.coerceAtLeast(1),
            numOfRows = numOfRows.coerceIn(1, 1000),
        )
        val response = requestRaw(url)
        if (!response.statusCode.is2xxSuccessful) {
            if (response.statusCode.value() == 401) {
                if (authCooldown.markUnauthorized(API_NAME)) {
                    log.warn("{} API 인증 실패(401). 서비스키 승인/인코딩 상태를 확인하세요. 5분 동안 재호출을 생략합니다.", API_NAME)
                }
                return emptyList()
            }
            log.warn("주차장 혼잡도 HTTP 오류. status={}, body={}", response.statusCode.value(), response.body.take(300))
            return emptyList()
        }
        if (response.body.contains("<OpenAPI_ServiceResponse>", ignoreCase = true)) {
            log.warn("주차장 혼잡도 공공데이터 에러 XML 응답. body={}", response.body.take(300))
            return emptyList()
        }
        return parseXml(response.body)
    }

    private fun parseXml(xml: String): List<ParkingCongestion> {
        return runCatching {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = false
            val doc = factory.newDocumentBuilder().parse(InputSource(StringReader(xml)))
            val header = doc.getElementsByTagName("header").item(0) as? Element
            val resultCode = header?.childText("resultCode").orEmpty()
            if (resultCode.isNotBlank() && resultCode != "00") {
                log.warn(
                    "주차장 혼잡도 기관 오류. resultCode={}, resultMsg={}",
                    resultCode,
                    header?.childText("resultMsg").orEmpty(),
                )
                return emptyList()
            }

            val itemNodes = doc.getElementsByTagName("item")
            (0 until itemNodes.length).mapNotNull { idx ->
                val item = itemNodes.item(idx) as? Element ?: return@mapNotNull null
                val floor = item.childText("floor")?.trim().orEmpty()
                if (floor.isBlank()) return@mapNotNull null
                ParkingCongestion(
                    floor = floor,
                    terminal = floor.toTerminal(),
                    parking = item.childText("parking").toIntFromDecimal(),
                    parkingArea = item.childText("parkingarea").toIntFromDecimal(),
                    observedAt = parseObservedAt(item.childText("datetm")),
                )
            }
        }.getOrElse {
            log.warn("주차장 혼잡도 XML 파싱 실패: {}", it.message)
            emptyList()
        }
    }

    private fun buildUrl(
        encodedServiceKey: String,
        pageNo: Int,
        numOfRows: Int,
    ): String {
        val query = buildList {
            add("serviceKey=$encodedServiceKey")
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
            log.warn("주차장 혼잡도 API 호출 실패: {}", e.message)
            RawHttpResponse(HttpStatusCode.valueOf(502), e.message ?: "")
        }
    }

    private fun resolveEncodedServiceKey(rawServiceKey: String): String {
        val trimmed = rawServiceKey.trim().removePrefix("serviceKey=").removePrefix("AIRPORT_PARKING_SERVICE_KEY=")
        if (trimmed.isBlank()) return ""
        return if (trimmed.contains('%')) trimmed else UriUtils.encodeQueryParam(trimmed, StandardCharsets.UTF_8)
    }

    private fun parseObservedAt(value: String?): LocalDateTime? {
        if (value.isNullOrBlank()) return null
        return runCatching { LocalDateTime.parse(value.trim(), DATE_TIME_FORMATTER_MILLIS) }
            .recoverCatching { LocalDateTime.parse(value.trim().substringBefore('.'), DATE_TIME_FORMATTER_PLAIN) }
            .getOrNull()
    }

    private fun String?.toIntFromDecimal(): Int? {
        if (this.isNullOrBlank()) return null
        return this.trim().toDoubleOrNull()?.toInt()
    }

    private fun String.toTerminal(): String? =
        when {
            startsWith("T1", ignoreCase = true) -> "T1"
            startsWith("T2", ignoreCase = true) -> "T2"
            else -> null
        }

    private fun Element.childText(tag: String): String? {
        val node = this.getElementsByTagName(tag).item(0) as? Element ?: return null
        return node.textContent?.trim()?.takeIf { it.isNotEmpty() }
    }

    companion object {
        private const val API_NAME = "주차장 혼잡도"
        private val DATE_TIME_FORMATTER_MILLIS = DateTimeFormatter.ofPattern("yyyyMMddHHmmss.SSS")
        private val DATE_TIME_FORMATTER_PLAIN = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }
}
