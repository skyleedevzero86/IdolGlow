package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

data class LlmSubwayStationSummary(
    val title: String? = null,
    val bullets: List<String>? = null,
    val learnMoreLabel: String? = null,
) {
    fun normalize(stationName: String): LlmSubwayStationSummary {
        val t = title?.trim()?.takeIf { it.isNotEmpty() }
        val bs = (bullets ?: emptyList()).map { it.trim() }.filter { it.isNotEmpty() }.take(3)
        val learn = learnMoreLabel?.trim()?.takeIf { it.isNotEmpty() }
            ?: "${stationName}에 대해 더 알아보세요"
        return LlmSubwayStationSummary(title = t, bullets = bs, learnMoreLabel = learn)
    }
}
