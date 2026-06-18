package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpStatusCode

data class RawSubwayOpenAiResponse(
    val statusCode: HttpStatusCode,
    val body: String,
)

data class SubwayChatCompletionRequest(
    val model: String,
    val temperature: Double,
    val messages: List<SubwayChatMessage>,
    @JsonProperty("response_format")
    val responseFormat: SubwayResponseFormat,
)

data class SubwayChatMessage(
    val role: String,
    val content: String,
)

data class SubwayResponseFormat(
    val type: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SubwayChatCompletionResponse(
    val choices: List<SubwayChatChoice> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SubwayChatChoice(
    val message: SubwayChatChoiceMessage = SubwayChatChoiceMessage(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SubwayChatChoiceMessage(
    val content: String? = null,
)
