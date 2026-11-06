package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import com.sleekydz86.idolglow.global.llm.LlmGateway
import com.sleekydz86.idolglow.global.llm.LlmRequest
import com.sleekydz86.idolglow.subway.domain.SubwayStationFacts
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper
import kotlin.math.abs
import kotlin.math.max

@Component
class SubwayStationSummaryLlmRouter(
    private val llmGateway: LlmGateway,
    private val objectMapper: ObjectMapper,
    private val factsAssembler: SubwayStationFactsAssembler,
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
        val facts =
            factsAssembler
                .takeIf { summaryEnabled }
                ?.assemble(lineId, lineName, stationCd, stationDisplayName)
                ?.takeIf { it.nearbyPlaces.isNotEmpty() }
        return facts?.let(::write)?.let { LlmSubwayStationSummary(bullets = it) }
    }

    private fun write(facts: SubwayStationFacts): List<String>? =
        llmGateway
            .complete(
                LlmRequest(
                    feature = FEATURE,
                    systemPrompt = SubwayStationSummaryPrompts.system,
                    userPrompt = SubwayStationSummaryPrompts.userJson(objectMapper, facts),
                    temperature = TEMPERATURE,
                ),
            )?.let { result ->
                parseBullets(result.text)
                    .takeIf { grounded(it, facts) }
                    .also { if (it == null) logDropped(result.provider, result.text) }
            }

    private fun parseBullets(text: String): List<String> {
        val json =
            text
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        return runCatching {
            val node = objectMapper.readTree(json).path("bullets")
            (0 until node.size())
                .map { node.path(it).asText("").trim() }
                .filter { it.isNotEmpty() }
        }.getOrDefault(emptyList())
    }

    private fun grounded(
        bullets: List<String>,
        facts: SubwayStationFacts,
    ): Boolean {
        val names =
            (facts.nearbyPlaces.map { it.name } + facts.neighborStations + facts.transferLines)
                .plus(listOf(facts.station, facts.line))
                .sortedByDescending { it.length }
        val distances = facts.nearbyPlaces.map { it.distanceMeters }
        return bullets.size == BULLET_COUNT &&
            bullets.all { bullet ->
                val withoutNames = names.fold(bullet) { acc, name -> acc.replace(name, " ") }
                !FOREIGN_LETTERS.containsMatchIn(withoutNames) &&
                    METERS.findAll(withoutNames).all { m -> closeToFact(m.groupValues[1].toInt(), distances) }
            }
    }

    private fun closeToFact(
        meters: Int,
        distances: List<Int>,
    ): Boolean =
        distances.any { fact ->
            abs(fact - meters) <= max(DISTANCE_TOLERANCE_METERS, fact * DISTANCE_TOLERANCE_PERCENT / PERCENT)
        }

    private fun logDropped(
        provider: String,
        text: String,
    ) {
        log.warn("지하철 역 요약 결과 버림 provider={} text={}", provider, text.take(LOG_TEXT_LIMIT))
    }

    companion object {
        const val CACHE_NAME: String = "subway-station-llm-summary"
        private const val FEATURE: String = "subway"
        private const val TEMPERATURE: Double = 0.2
        private const val BULLET_COUNT: Int = 3
        private const val LOG_TEXT_LIMIT: Int = 200
        private const val DISTANCE_TOLERANCE_METERS: Int = 60
        private const val DISTANCE_TOLERANCE_PERCENT: Int = 15
        private const val PERCENT: Int = 100
        private val FOREIGN_LETTERS = Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}]|[A-Za-z]{2,}")
        private val METERS = Regex("(\\d+)\\s*m")
    }
}
