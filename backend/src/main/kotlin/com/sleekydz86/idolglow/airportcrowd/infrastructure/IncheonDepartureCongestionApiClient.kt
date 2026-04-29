package com.sleekydz86.idolglow.airportcrowd.infrastructure

import com.sleekydz86.idolglow.airportcrowd.application.port.out.DepartureCongestionQueryPort
import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCongestion
import com.sleekydz86.idolglow.global.infrastructure.config.IncheonAirportCongestionProperties
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriUtils
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

@Component
class IncheonDepartureCongestionApiClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val properties: IncheonAirportCongestionProperties,
    private val authCooldown: IncheonAirportApiAuthCooldown,
) : DepartureCongestionQueryPort {
    private val log = LoggerFactory.getLogger(this::class.java)

    override fun fetchCurrent(
        terminalId: String?,
        gateId: String?,
        pageNo: Int,
        numOfRows: Int,
    ): List<DepartureCongestion> {
        if (authCooldown.isBlocked(API_NAME)) return emptyList()
        val encodedKey = resolveEncodedServiceKey(properties.serviceKey)
        if (encodedKey.isBlank()) return emptyList()
        val uri = buildUrl(
            encodedServiceKey = encodedKey,
            terminalId = terminalId,
            gateId = gateId,
            pageNo = pageNo.coerceAtLeast(1),
            numOfRows = numOfRows.coerceIn(1, 1000),
        )
        val rows = fetchByUrl(uri)
        if (rows.isNotEmpty()) return rows

        if (!terminalId.isNullOrBlank() || !gateId.isNullOrBlank()) {
            val fallbackUri = buildUrl(
                encodedServiceKey = encodedKey,
                terminalId = null,
                gateId = null,
                pageNo = pageNo.coerceAtLeast(1),
                numOfRows = numOfRows.coerceIn(1, 1000),
            )
            return fetchByUrl(fallbackUri).filter { item ->
                val terminalMatch = terminalId.isNullOrBlank() || item.terminalId.equals(terminalId, ignoreCase = true)
                val gateMatch = gateId.isNullOrBlank() || item.gateId.equals(gateId, ignoreCase = true)
                terminalMatch && gateMatch
            }
        }
        return emptyList()
    }

    private fun fetchByUrl(url: String): List<DepartureCongestion> {
        val response = requestRaw(url)
        if (!response.statusCode.is2xxSuccessful) {
            if (response.statusCode.value() == 401) {
                if (authCooldown.markUnauthorized(API_NAME)) {
                    log.warn("{} API 인증 실패(401). 서비스키 승인/인코딩 상태를 확인하세요. 5분 동안 재호출을 생략합니다.", API_NAME)
                }
                return emptyList()
            }
            log.warn("출국장 혼잡도 HTTP 오류. status={}, body={}", response.statusCode.value(), response.body.take(300))
            return emptyList()
        }
        if (response.body.contains("<OpenAPI_ServiceResponse>", ignoreCase = true)) {
            log.warn("출국장 혼잡도 공공데이터 에러 XML 응답. body={}", response.body.take(300))
            return emptyList()
        }
        val root = runCatching { objectMapper.readTree(response.body) }.getOrElse {
            log.warn("출국장 혼잡도 JSON 파싱 실패: {}", it.message)
            return emptyList()
        }
        val header = root.path("response").path("header")
        val resultCode = header.path("resultCode").asText("")
        if (resultCode.isNotBlank() && resultCode != "00") {
            log.warn(
                "출국장 혼잡도 기관 오류. resultCode={}, resultMsg={}",
                resultCode,
                header.path("resultMsg").asText(""),
            )
            return emptyList()
        }
        val itemsNode = root.path("response").path("body").path("items")
        val itemList = itemsNode.path("item").toNodeList()
        return itemList.mapNotNull { node ->
            val gate = node.path("gateId").asText("").trim()
            val terminal = node.path("terminalId").asText("").trim()
            if (gate.isEmpty() || terminal.isEmpty()) return@mapNotNull null
            DepartureCongestion(
                gateId = gate,
                terminalId = terminal,
                waitTimeMinutes = node.path("waitTime").asText("").trim().toIntOrNull(),
                waitLength = node.path("waitLength").asText("").trim().toIntOrNull(),
                occurredAt = parseOccurTime(node.path("occurtime").asText("").trim()),
                operatingTime = node.path("operatingTime").asText("").trim().takeIf { it.isNotEmpty() },
            )
        }
    }

    private fun parseOccurTime(raw: String): LocalDateTime? {
        if (raw.isBlank()) return null
        return runCatching { LocalDateTime.parse(raw, OCCUR_TIME_FORMATTER) }
            .recoverCatching {
                val normalized = if (raw.length >= 14) raw.take(14) else raw
                LocalDateTime.parse(normalized, OCCUR_TIME_FORMATTER)
            }
            .getOrElse {
                if (it is DateTimeParseException) {
                    log.debug("출국장 혼잡도 occurtime 파싱 실패. value={}", raw)
                }
                null
            }
    }

    private fun buildUrl(
        encodedServiceKey: String,
        terminalId: String?,
        gateId: String?,
        pageNo: Int,
        numOfRows: Int,
    ): String {
        val query = buildList {
            add("serviceKey=$encodedServiceKey")
            add("type=json")
            add("pageNo=$pageNo")
            add("numOfRows=$numOfRows")
            terminalId?.takeIf { it.isNotBlank() }?.let {
                add("terminalId=${UriUtils.encodeQueryParam(it, StandardCharsets.UTF_8)}")
            }
            gateId?.takeIf { it.isNotBlank() }?.let {
                add("gateId=${UriUtils.encodeQueryParam(it, StandardCharsets.UTF_8)}")
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
            log.warn("출국장 혼잡도 API 호출 실패: {}", e.message)
            RawHttpResponse(HttpStatusCode.valueOf(502), e.message ?: "")
        }
    }

    private fun resolveEncodedServiceKey(rawServiceKey: String): String {
        val trimmed = rawServiceKey.trim().removePrefix("serviceKey=").removePrefix("AIRPORT_CONGESTION_SERVICE_KEY=")
        if (trimmed.isBlank()) return ""
        return if (trimmed.contains('%')) trimmed else UriUtils.encodeQueryParam(trimmed, StandardCharsets.UTF_8)
    }

    private fun JsonNode.toNodeList(): List<JsonNode> =
        when {
            isMissingNode || isNull -> emptyList()
            isArray -> toList()
            isObject -> listOf(this)
            else -> emptyList()
        }

    companion object {
        private const val API_NAME = "출국장 혼잡도"
        private val OCCUR_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmss")
    }
}
