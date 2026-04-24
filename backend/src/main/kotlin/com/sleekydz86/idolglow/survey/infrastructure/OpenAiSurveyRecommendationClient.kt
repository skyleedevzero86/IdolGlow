package com.sleekydz86.idolglow.survey.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatusCode
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
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
                    content = """
                        한국 여행 추천 카피라이터 역할로 동작하세요.
                        반드시 JSON 객체만 출력하세요.
                        형식:
                        {
                          "title":"...",
                          "subtitle":"...",
                          "narrative":"...",
                          "attractionReasons":{"<attractionCode>":"..."}
                        }
                    """.trimIndent(),
                ),
                SurveyChatMessage(
                    role = "user",
                    content = objectMapper.writeValueAsString(
                        mapOf(
                            "fallback" to mapOf(
                                "title" to titleFallback,
                                "subtitle" to subtitleFallback,
                                "narrative" to narrativeFallback,
                            ),
                            "answerHighlights" to answerHighlights,
                            "attractions" to attractions.map {
                                mapOf(
                                    "attractionCode" to it.attractionCode,
                                    "name" to it.name,
                                    "areaName" to it.areaName,
                                    "signguName" to it.signguName,
                                    "categoryLarge" to it.categoryLarge,
                                    "categoryMiddle" to it.categoryMiddle,
                                    "rank" to it.rank,
                                )
                            },
                        )
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
            log.warn("Survey OpenAI call failed: {}", exception.message)
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

private data class RawSurveyOpenAiResponse(
    val statusCode: HttpStatusCode,
    val body: String,
)

private data class SurveyChatCompletionRequest(
    val model: String,
    val temperature: Double,
    val messages: List<SurveyChatMessage>,
    @JsonProperty("response_format")
    val responseFormat: SurveyResponseFormat,
)

private data class SurveyChatMessage(
    val role: String,
    val content: String,
)

private data class SurveyResponseFormat(
    val type: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SurveyChatCompletionResponse(
    val choices: List<SurveyChatChoice> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SurveyChatChoice(
    val message: SurveyChatChoiceMessage = SurveyChatChoiceMessage(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SurveyChatChoiceMessage(
    val content: String? = null,
)
