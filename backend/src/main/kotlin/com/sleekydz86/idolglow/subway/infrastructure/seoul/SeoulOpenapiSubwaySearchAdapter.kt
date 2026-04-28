package com.sleekydz86.idolglow.subway.infrastructure.seoul

import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.global.infrastructure.config.SubwayProperties
import com.sleekydz86.idolglow.subway.application.port.out.SubwayExternalStationSearchPort
import com.sleekydz86.idolglow.subway.domain.SubwayExternalSearchHit
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.util.UriComponentsBuilder
import java.net.URI
import java.nio.charset.StandardCharsets

@Component
class SeoulOpenapiSubwaySearchAdapter(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val properties: SubwayProperties,
) : SubwayExternalStationSearchPort {

    private val log = LoggerFactory.getLogger(javaClass)

    override fun searchByStationName(stationName: String): List<SubwayExternalSearchHit> {
        val key = properties.seoulApiKey.trim().ifEmpty { "sample" }
        val base = properties.baseUrl.trim().trimEnd('/')
        val encodedName = java.net.URLEncoder.encode(stationName, StandardCharsets.UTF_8)
        val endIndex = if (key == "sample") 5 else 100
        val uri: URI = UriComponentsBuilder.fromUriString(
            "$base/$key/json/SearchInfoBySubwayNameService/1/$endIndex/$encodedName/",
        ).build(true).toUri()

        val body = try {
            webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
        } catch (e: Exception) {
            log.warn("서울시 지하철 역명 검색 API 호출에 실패했습니다: {}", e.message)
            return emptyList()
        } ?: return emptyList()

        val root = try {
            objectMapper.readTree(body)
        } catch (e: Exception) {
            log.warn("서울시 지하철 역명 검색 API 응답 파싱에 실패했습니다: {}", e.message)
            return emptyList()
        }

        val svc = root.get("SearchInfoBySubwayNameService") ?: return emptyList()
        val result = svc.get("RESULT") ?: return emptyList()
        val code = result.get("CODE")?.asText().orEmpty()
        if (code != "INFO-000") {
            return emptyList()
        }

        val rowNode = svc.get("row") ?: return emptyList()
        val rows = if (rowNode.isArray) rowNode else objectMapper.createArrayNode().add(rowNode)

        val out = mutableListOf<SubwayExternalSearchHit>()
        for (r in rows) {
            val cd = r.get("STATION_CD")?.asText()?.trim().orEmpty()
            val nm = r.get("STATION_NM")?.asText()?.trim().orEmpty()
            val lineNum = r.get("LINE_NUM")?.asText()?.trim().orEmpty()
            val fr = r.get("FR_CODE")?.asText()?.trim().orEmpty()
            if (cd.isNotEmpty() && nm.isNotEmpty()) {
                out.add(
                    SubwayExternalSearchHit(
                        stationCd = cd,
                        stationName = nm,
                        lineNumLabel = lineNum,
                        frCode = fr,
                    ),
                )
            }
        }
        return out
    }
}
