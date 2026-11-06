package com.sleekydz86.idolglow.global.llm

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Component
class OpenAiLlmProvider(
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
) : LlmProvider {
    override val name = "openai"
    override val external = true

    override fun isConfigured(): Boolean = enabled && apiKey.isNotBlank()

    override fun complete(request: LlmRequest): String? {
        val payload = OpenAiCompatiblePayload.build(objectMapper, model, request)
        val raw =
            webClient
                .post()
                .uri("${baseUrl.trimEnd('/')}/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${apiKey.trim()}")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(objectMapper.writeValueAsString(payload))
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
                ?: return null
        return OpenAiCompatiblePayload.firstMessageContent(objectMapper, raw)
    }
}
