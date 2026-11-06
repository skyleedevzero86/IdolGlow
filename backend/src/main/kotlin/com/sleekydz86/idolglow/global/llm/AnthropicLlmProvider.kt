package com.sleekydz86.idolglow.global.llm

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Component
class AnthropicLlmProvider(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    @Value("\${app.llm.anthropic.enabled:false}")
    private val enabled: Boolean,
    @Value("\${app.llm.anthropic.base-url:https://api.anthropic.com}")
    private val baseUrl: String,
    @Value("\${app.llm.anthropic.api-key:}")
    private val apiKey: String,
    @Value("\${app.llm.anthropic.model:claude-3-5-haiku-20241022}")
    private val model: String,
) : LlmProvider {
    override val name = "anthropic"
    override val external = true

    override fun isConfigured(): Boolean = enabled && apiKey.isNotBlank()

    override fun complete(request: LlmRequest): String? {
        val payload =
            objectMapper.createObjectNode().apply {
                put("model", model)
                put("max_tokens", request.maxOutputTokens)
                put("temperature", request.temperature)
                put("system", request.systemPrompt)
                set(
                    "messages",
                    objectMapper.createArrayNode().apply {
                        add(objectMapper.createObjectNode().put("role", "user").put("content", request.userPrompt))
                    },
                )
            }
        val raw =
            webClient
                .post()
                .uri("${baseUrl.trimEnd('/')}/v1/messages")
                .header("x-api-key", apiKey.trim())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(objectMapper.writeValueAsString(payload))
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
                ?: return null
        return objectMapper
            .readTree(raw)
            .path("content")
            .firstOrNull { it.path("type").asText("") == "text" }
            ?.path("text")
            ?.asText("")
            ?.takeIf { it.isNotBlank() }
    }

    companion object {
        private const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
