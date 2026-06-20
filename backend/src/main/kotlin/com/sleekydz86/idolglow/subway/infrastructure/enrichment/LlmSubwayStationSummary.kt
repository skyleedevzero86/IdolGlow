package com.sleekydz86.idolglow.subway.infrastructure.enrichment

data class LlmSubwayStationSummary(
    val title: String? = null,
    val bullets: List<String>? = null,
    val learnMoreLabel: String? = null,
) {
    fun normalize(stationName: String): LlmSubwayStationSummary {
        val t = title?.trim()?.takeIf { it.isNotEmpty() }
        val bs = (bullets ?: emptyList()).map { it.trim() }.filter { it.isNotEmpty() }.take(3)
        val learn =
            learnMoreLabel?.trim()?.takeIf { it.isNotEmpty() }
                ?: "${stationName}에 대해 더 알아보세요"
        return LlmSubwayStationSummary(title = t, bullets = bs, learnMoreLabel = learn)
    }
}
