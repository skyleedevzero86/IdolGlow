package com.sleekydz86.idolglow.glowweather.infrastructure

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component

@Component
class GlowClimateWindRepository(
    private val objectMapper: ObjectMapper,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    private val byRegion: Map<String, Map<Int, ClimateWindCell>> = load()

    data class ClimateWindCell(
        @JsonProperty("dir") val dir: String,
        @JsonProperty("mps") val mps: Double,
    )

    fun month(regionId: String, month: Int): ClimateWindCell? {
        if (month !in 1..12) return null
        return byRegion[regionId.trim().lowercase()]?.get(month)
    }

    private fun load(): Map<String, Map<Int, ClimateWindCell>> {
        val res = ClassPathResource("data/glow-weather-climate-wind.json")
        if (!res.exists()) {
            log.warn("glow-weather-climate-wind.json 파일이 없어 기후통계 풍향 보완을 사용하지 않습니다.")
            return emptyMap()
        }
        return runCatching {
            res.inputStream.use { stream ->
                val type = object : TypeReference<Map<String, Map<String, ClimateWindCell>>>() {}
                val raw: Map<String, Map<String, ClimateWindCell>> = objectMapper.readValue(stream, type)
                raw.mapValues { (_, months) ->
                    months.mapNotNull { (k, v) ->
                        k.toIntOrNull()?.let { m -> m to v }
                    }.toMap()
                }
            }
        }.getOrElse { e ->
            log.warn("glow-weather-climate-wind.json 로드 실패: {}", e.message)
            emptyMap()
        }
    }
}
