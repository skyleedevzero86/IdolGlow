package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component

@Component
class SubwayStationSummaryLlmRouter(
    private val openAiSubwayStationSummaryClient: OpenAiSubwayStationSummaryClient,
    private val anthropicSubwayStationSummaryClient: AnthropicSubwayStationSummaryClient,
    @Value("\${app.llm.subway.openai-summary:true}")
    private val summaryEnabled: Boolean,
    @Value("\${app.llm.subway.provider:auto}")
    private val providerRaw: String,
) {

    @Cacheable(
        cacheNames = [OpenAiSubwayStationSummaryClient.CACHE_NAME],
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
        val p = providerRaw.trim().lowercase()
        return when (p) {
            "openai", "gpt" -> openAiSubwayStationSummaryClient.generate(lineId, lineName, stationCd, stationDisplayName)
            "anthropic", "claude" ->
                anthropicSubwayStationSummaryClient.generate(lineId, lineName, stationCd, stationDisplayName)
            else ->
                openAiSubwayStationSummaryClient.generate(lineId, lineName, stationCd, stationDisplayName)
                    ?: anthropicSubwayStationSummaryClient.generate(lineId, lineName, stationCd, stationDisplayName)
        }
    }
}
