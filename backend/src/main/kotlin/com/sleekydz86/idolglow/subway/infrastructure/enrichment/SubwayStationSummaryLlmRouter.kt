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
        val english =
            askBullets(
                systemPrompt = SubwayStationSummaryPrompts.system,
                userPrompt =
                    SubwayStationSummaryPrompts.userJson(objectMapper, lineId, lineName, stationCd, stationDisplayName),
                temperature = WRITE_TEMPERATURE,
                forbidden = CJK,
            )
        val korean =
            english?.let {
                askBullets(
                    systemPrompt = SubwayStationSummaryPrompts.translateToKorean,
                    userPrompt = SubwayStationSummaryPrompts.bulletsJson(objectMapper, it),
                    temperature = TRANSLATE_TEMPERATURE,
                    forbidden = NOT_KOREAN,
                )
            }
        return korean?.let { LlmSubwayStationSummary(bullets = it) }
    }

    private fun askBullets(
        systemPrompt: String,
        userPrompt: String,
        temperature: Double,
        forbidden: Regex,
    ): List<String>? =
        llmGateway
            .complete(
                LlmRequest(
                    feature = FEATURE,
                    systemPrompt = systemPrompt,
                    userPrompt = userPrompt,
                    temperature = temperature,
                ),
            )?.let { parseBullets(it.text, it.provider, forbidden) }

    private fun parseBullets(
        text: String,
        provider: String,
        forbidden: Regex,
    ): List<String>? {
        val json =
            text
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        val bullets =
            runCatching {
                val node = objectMapper.readTree(json).path("bullets")
                (0 until node.size())
                    .map { node.path(it).asText("").trim() }
                    .filter { it.isNotEmpty() }
                    .take(BULLET_COUNT)
            }.getOrNull()
                .orEmpty()
        val valid = bullets.size == BULLET_COUNT && bullets.none { forbidden.containsMatchIn(it) }
        if (!valid) {
            log.warn("지하철 역 요약 결과 버림 provider={} text={}", provider, text.take(LOG_TEXT_LIMIT))
        }
        return bullets.takeIf { valid }
    }

    companion object {
        const val CACHE_NAME: String = "subway-station-llm-summary"
        private const val FEATURE: String = "subway"
        private const val WRITE_TEMPERATURE: Double = 0.45
        private const val TRANSLATE_TEMPERATURE: Double = 0.1
        private const val BULLET_COUNT: Int = 3
        private const val LOG_TEXT_LIMIT: Int = 200
        private val CJK = Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}]")
        private val NOT_KOREAN = Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}A-Za-z]")
    }
}
