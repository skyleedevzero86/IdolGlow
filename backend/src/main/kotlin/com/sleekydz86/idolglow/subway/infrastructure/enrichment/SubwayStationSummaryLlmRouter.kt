package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import com.sleekydz86.idolglow.global.llm.LlmGateway
import com.sleekydz86.idolglow.global.llm.LlmRequest
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class SubwayStationSummaryLlmRouter(
    private val llmGateway: LlmGateway,
    private val objectMapper: ObjectMapper,
    @Value("\${app.llm.subway.openai-summary:true}")
    private val summaryEnabled: Boolean,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Cacheable(
        cacheNames = [CACHE_NAME],
        key = "#lineId + ':' + #stationCd",
        unless = "#result == null",
    )
    fun generateSummary(
        lineId: String,
        lineName: String,
        stationCd: String,
        stationDisplayName: String,
    ): LlmSubwayStationSummary? {
        if (!summaryEnabled) {
            return null
        }
        return llmGateway
            .complete(
                LlmRequest(
                    feature = FEATURE,
                    systemPrompt = SubwayStationSummaryPrompts.system,
                    userPrompt =
                        SubwayStationSummaryPrompts.userJson(
                            objectMapper,
                            lineId,
                            lineName,
                            stationCd,
                            stationDisplayName,
                        ),
                    temperature = TEMPERATURE,
                ),
            )?.let { parse(it.text, it.provider, stationDisplayName) }
    }

    private fun parse(
        text: String,
        provider: String,
        stationDisplayName: String,
    ): LlmSubwayStationSummary? {
        val json =
            text
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        return runCatching {
            objectMapper.readValue(json, LlmSubwayStationSummary::class.java).normalize(stationDisplayName)
        }.getOrElse { e ->
            log.warn("지하철 역 요약 JSON 파싱 실패 provider={} error={}", provider, e.message)
            null
        }
    }

    companion object {
        const val CACHE_NAME: String = "subway-station-llm-summary"
        private const val FEATURE: String = "subway"
        private const val TEMPERATURE: Double = 0.45
    }
}
