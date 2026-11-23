package com.sleekydz86.idolglow.groundedsearch.infrastructure

import org.springframework.context.annotation.Profile
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.server.ResponseStatusException
import tools.jackson.databind.ObjectMapper

@Component
@Profile("!test")
class GroundedAnthropicGateway(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val settings: GroundedRemoteSettings,
    private val guard: GroundedCallGuard,
) {
    fun complete(system: String, user: String): String? {
        if (!ready()) {
            return null
        }
        val body = guard.run("Anthropic") { post(system, user) }
        return text(body)
    }

    private fun post(system: String, user: String): String =
        webClient
            .post()
            .uri("${settings.text(BASE_URL).trimEnd('/')}/v1/messages")
            .header("x-api-key", settings.text(API_KEY))
            .header("anthropic-version", ANTHROPIC_VERSION)
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .bodyValue(
                GroundedAnthropicRequest(
                    model = settings.text(MODEL),
                    messages = listOf(GroundedLlmMessage("user", user)),
                    system = system,
                    maxTokens = 2 shl 8,
                ),
            ).retrieve()
            .bodyToMono(String::class.java)
            .timeout(settings.llmTimeout())
            .block()
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 언어 모델 응답이 비어 있습니다.")

    private fun text(body: String): String =
        objectMapper.readTree(body).path("content").path(0).path("text").asText()

    private fun ready(): Boolean = settings.text(ENABLED).toBoolean() && settings.text(API_KEY).isNotBlank()

    private companion object {
        private const val ENABLED = "app.llm.anthropic.enabled"
        private const val API_KEY = "app.llm.anthropic.api-key"
        private const val BASE_URL = "app.llm.anthropic.base-url"
        private const val MODEL = "app.llm.anthropic.model"
        private const val ANTHROPIC_VERSION = "2023-06-01"
    }
}
