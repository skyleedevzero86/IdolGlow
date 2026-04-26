package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Component
class AnthropicSubwayStationSummaryClient(
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
        lineId: String,
        lineName: String,
        stationCd: String,
        stationDisplayName: String,
    ): LlmSubwayStationSummary? {
        if (!anthropicEnabled) {
            return null
        }
        val normalizedApiKey = apiKey.trim()
        if (normalizedApiKey.isEmpty()) {
            return null
        }

        val userContent = SubwayStationSummaryPrompts.userJson(
            objectMapper,
            lineId,
            lineName,
            stationCd,
            stationDisplayName,
        )

        val request = AnthropicMessagesRequest(
            model = model,
            maxTokens = 1024,
            temperature = 0.45,
            system = SubwayStationSummaryPrompts.system,
            messages = listOf(AnthropicMessage(role = "user", content = userContent)),
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
            log.warn("Subway Anthropic 요약 호출 실패: {}", e.message)
            return null
        } ?: return null

        val text = runCatching {
            objectMapper.readValue(body, AnthropicMessagesResponse::class.java)
                .content.firstOrNull { it.type == "text" }?.text
        }.getOrNull() ?: return null

        val json = text.trim()
            .removePrefix("```json")
            .removePrefix("```")
            .removeSuffix("```")
            .trim()

        return runCatching {
            objectMapper.readValue(json, LlmSubwayStationSummary::class.java).normalize(stationDisplayName)
        }.getOrElse { e ->
            log.warn("Subway Anthropic JSON 파싱 실패: {}", e.message)
            null
        }
    }

    companion object {
        private const val ANTHROPIC_VERSION: String = "2023-06-01"
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
private data class AnthropicMessagesRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    val system: String,
    val temperature: Double,
    @JsonProperty("max_tokens")
    val maxTokens: Int,
)

private data class AnthropicMessage(
    val role: String,
    val content: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class AnthropicMessagesResponse(
    val content: List<AnthropicContentBlock> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
private data class AnthropicContentBlock(
    val type: String? = null,
    val text: String? = null,
)
