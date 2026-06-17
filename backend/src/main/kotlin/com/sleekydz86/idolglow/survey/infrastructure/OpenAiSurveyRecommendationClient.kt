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

class OpenAiSurveyRecommendationClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    @Value("\${app.llm.openai.enabled:false}")
    private val enabled: Boolean,
    @Value("\${app.llm.openai.base-url:https://api.openai.com}")
    private val baseUrl: String,
    @Value("\${app.llm.openai.api-key:}")
    private val apiKey: String,
    @Value("\${app.llm.openai.model:gpt-4.1-mini}")
    private val model: String,
) {
    private val log = LoggerFactory.getLogger(this::class.java)

    fun generate(
        titleFallback: String,
        subtitleFallback: String,
        narrativeFallback: String,
        answerHighlights: List<String>,
        attractions: List<TourAttraction>,
    ): LlmSurveyRecommendation? {
        if (!enabled) return null
        val normalizedApiKey = apiKey.trim()
        if (normalizedApiKey.isEmpty()) return null

        val request = SurveyChatCompletionRequest(
            model = model,
            temperature = 0.3,
            responseFormat = SurveyResponseFormat(type = "json_object"),
            messages = listOf(
                SurveyChatMessage(
                    role = "system",
                    content = SurveyLlmPrompts.system,
                ),
                SurveyChatMessage(
                    role = "user",
                    content = SurveyLlmPrompts.userJson(
                        objectMapper,
                        titleFallback,
                        subtitleFallback,
                        narrativeFallback,
                        answerHighlights,
                        attractions,
                    ),
                ),
            ),
        )

        val raw = try {
            webClient.post()
                .uri("${baseUrl.trimEnd('/')}/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $normalizedApiKey")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(request)
                .exchangeToMono { response ->
                    response.bodyToMono(String::class.java)
                        .defaultIfEmpty("")
                        .map { body -> RawSurveyOpenAiResponse(response.statusCode(), body) }
                }
                .block()
        } catch (exception: Exception) {
            log.warn("설문 추천 OpenAI 호출 실패: {}", exception.message)
            null
        } ?: return null

        if (!raw.statusCode.is2xxSuccessful) {
            return null
        }
        val content = runCatching {
            objectMapper.readValue(raw.body, SurveyChatCompletionResponse::class.java)
                .choices.firstOrNull()?.message?.content
        }.getOrNull() ?: return null

        val json = content.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
        return runCatching { objectMapper.readValue(json, LlmSurveyRecommendation::class.java).normalize() }.getOrNull()
    }
}
