package com.sleekydz86.idolglow.survey.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import tools.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

data class LlmSurveyRecommendation(
    val title: String? = null,
    val subtitle: String? = null,
    val narrative: String? = null,
    val attractionReasons: Map<String, String> = emptyMap(),
) {
    fun normalize(): LlmSurveyRecommendation = copy(
        title = title?.trim()?.takeIf { it.isNotEmpty() },
        subtitle = subtitle?.trim()?.takeIf { it.isNotEmpty() },
        narrative = narrative?.trim()?.takeIf { it.isNotEmpty() },
        attractionReasons = attractionReasons
            .mapValues { (_, value) -> value.trim() }
            .filterValues { it.isNotEmpty() },
    )
}
