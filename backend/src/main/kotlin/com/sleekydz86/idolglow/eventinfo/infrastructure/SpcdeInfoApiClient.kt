package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.domain.SpecialDayInfo
import com.sleekydz86.idolglow.global.infrastructure.config.SpcdeInfoApiProperties
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriUtils
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.nio.charset.StandardCharsets

@Component
class SpcdeInfoApiClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val spcdeInfoApiProperties: SpcdeInfoApiProperties,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun getHoliDeInfo(solYear: String, solMonth: String): List<SpecialDayInfo> =
        fetch("getHoliDeInfo", solYear, solMonth, "SPCDE_HOLIDE_INFO")

    fun getRestDeInfo(solYear: String, solMonth: String): List<SpecialDayInfo> =
        fetch("getRestDeInfo", solYear, solMonth, "SPCDE_RESTDE_INFO")

    fun getAnniversaryInfo(solYear: String, solMonth: String): List<SpecialDayInfo> =
        fetch("getAnniversaryInfo", solYear, solMonth, "SPCDE_ANNIVERSARY_INFO")

    private fun fetch(operation: String, solYear: String, solMonth: String, source: String): List<SpecialDayInfo> {
        val serviceKey = spcdeInfoApiProperties.serviceKey.trim()
        if (serviceKey.isEmpty()) return emptyList()
        val url = buildUrl(operation, serviceKey, solYear, solMonth)
        val body = runCatching {
            webClient.get().uri(url).retrieve().bodyToMono(String::class.java).block()
        }.getOrElse {
            log.warn("특일 정보 API 호출 실패. op={}, message={}", operation, it.message)
            null
        } ?: return emptyList()
        val root = runCatching { objectMapper.readTree(body) }.getOrElse {
            log.warn("특일 정보 API 파싱 실패. op={}, message={}", operation, it.message)
            return emptyList()
        }
        val resultCode = root.path("response").path("header").path("resultCode").asText("")
        if (resultCode != "00") {
            log.warn(
                "특일 정보 API 오류. op={}, resultCode={}, resultMsg={}",
                operation,
                resultCode,
                root.path("response").path("header").path("resultMsg").asText(""),
            )
            return emptyList()
        }
        val itemNode = root.path("response").path("body").path("items").path("item")
        if (itemNode.isMissingNode || itemNode.isNull) return emptyList()
        val rows = if (itemNode.isArray) itemNode.toList() else listOf(itemNode)
        return rows.mapNotNull { toSpecialDayInfo(it, source) }
    }

    private fun toSpecialDayInfo(node: JsonNode, source: String): SpecialDayInfo? {
        val dateName = node.path("dateName").asText("").trim()
        val locDate = node.path("locdate").asText("").trim()
        if (dateName.isEmpty() || locDate.isEmpty()) return null
        return SpecialDayInfo(
            dateName = dateName,
            locDate = locDate,
            dateKind = node.path("dateKind").asText("").ifBlank { null },
            isHoliday = node.path("isHoliday").asText("").ifBlank { null },
            seq = node.path("seq").asText("").toIntOrNull(),
            source = source,
        )
    }

    private fun buildUrl(
        operation: String,
        serviceKey: String,
        solYear: String,
        solMonth: String,
    ): String {
        val qKey = UriUtils.encodeQueryParam(serviceKey, StandardCharsets.UTF_8)
        val qYear = UriUtils.encodeQueryParam(solYear, StandardCharsets.UTF_8)
        val qMonth = UriUtils.encodeQueryParam(solMonth, StandardCharsets.UTF_8)
        return "${spcdeInfoApiProperties.baseUrl.trimEnd('/')}/$operation" +
            "?ServiceKey=$qKey" +
            "&solYear=$qYear" +
            "&solMonth=$qMonth" +
            "&_type=json" +
            "&numOfRows=100"
    }
}
