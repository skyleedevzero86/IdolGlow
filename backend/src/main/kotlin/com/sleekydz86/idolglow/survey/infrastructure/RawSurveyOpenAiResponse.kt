package com.sleekydz86.idolglow.survey.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.springframework.http.HttpStatusCode

data class RawSurveyOpenAiResponse(
    val statusCode: HttpStatusCode,
    val body: String,
)

data class SurveyChatCompletionRequest(
    val model: String,
    val temperature: Double,
    val messages: List<SurveyChatMessage>,
    @JsonProperty("response_format")
    val responseFormat: SurveyResponseFormat,
)

data class SurveyChatMessage(
    val role: String,
    val content: String,
)

data class SurveyResponseFormat(
    val type: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SurveyChatCompletionResponse(
    val choices: List<SurveyChatChoice> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SurveyChatChoice(
    val message: SurveyChatChoiceMessage = SurveyChatChoiceMessage(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class SurveyChatChoiceMessage(
    val content: String? = null,
)
