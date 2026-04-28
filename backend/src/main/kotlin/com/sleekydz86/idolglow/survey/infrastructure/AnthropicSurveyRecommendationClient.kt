package com.sleekydz86.idolglow.survey.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Component
class AnthropicSurveyRecommendationClient(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    @Value("\${app.llm.anthropic.enabled:false}")
    private val anthropicEnabled: Boolean,
    @Value("\${app.llm.anthropic.base-url:https://api.anthropic.com}")
    private val baseUrl: String,
    @Value("\${app.llm.anthropic.api-key:}")
    private val apiKey: String,
    @Value("\${app.llm.anthropic.model:claude-3-5-haiku-20241022}")
    private val model: String,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(
        titleFallback: String,
        subtitleFallback: String,
        narrativeFallback: String,
        answerHighlights: List<String>,
        attractions: List<TourAttraction>,
    ): LlmSurveyRecommendation? {
        if (!anthropicEnabled) {
            return null
        }
        val normalizedApiKey = apiKey.trim()
        if (normalizedApiKey.isEmpty()) {
            return null
        }

        val userContent = SurveyLlmPrompts.userJson(
            objectMapper,
            titleFallback,
            subtitleFallback,
            narrativeFallback,
            answerHighlights,
            attractions,
        )

        val request = SurveyAnthropicMessagesRequest(
            model = model,
            maxTokens = 2048,
            temperature = 0.3,
            system = SurveyLlmPrompts.system,
            messages = listOf(SurveyAnthropicMessage(role = "user", content = userContent)),
        )

        val body = try {
            webClient.post()
                .uri("${baseUrl.trimEnd('/')}/v1/messages")
                .header("x-api-key", normalizedApiKey)
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
        } catch (e: Exception) {
            log.warn("Survey Anthropic call failed: {}", e.message)
            return null
        } ?: return null

        val text = runCatching {
            objectMapper.readValue(body, SurveyAnthropicMessagesResponse::class.java)
                .content.firstOrNull { it.type == "text" }?.text
        }.getOrNull() ?: return null

        val json = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return runCatching {
            objectMapper.readValue(json, LlmSurveyRecommendation::class.java).normalize()
        }.getOrElse { e ->
            log.warn("Survey Anthropic JSON parse failed: {}", e.message)
            null
        }
    }

    companion object {
        private const val ANTHROPIC_VERSION: String = "2023-06-01"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SurveyAnthropicMessagesRequest(
    val model: String,
    val messages: List<SurveyAnthropicMessage>,
    val system: String,
    val temperature: Double,
    @JsonProperty("max_tokens")
    val maxTokens: Int,
)

private data class SurveyAnthropicMessage(
    val role: String,
    val content: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SurveyAnthropicMessagesResponse(
    val content: List<SurveyAnthropicContentBlock> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class SurveyAnthropicContentBlock(
    val type: String? = null,
    val text: String? = null,
)
