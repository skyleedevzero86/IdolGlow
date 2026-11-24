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
class GroundedOpenAiGateway(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val settings: GroundedRemoteSettings,
    private val guard: GroundedCallGuard,
) {
    fun complete(system: String, user: String, json: Boolean): String? {
        if (!ready()) {
            return null
        }
        val body = guard.run("OpenAI") { post(system, user, json) }
        return text(body)
    }

    private fun post(system: String, user: String, json: Boolean): String {
        val format = if (json) GroundedResponseFormat("json_object") else null
        return webClient
            .post()
            .uri("${settings.text(BASE_URL).trimEnd('/')}/v1/chat/completions")
            .header(HttpHeaders.AUTHORIZATION, "Bearer ${settings.text(API_KEY)}")
            .header(HttpHeaders.CONTENT_TYPE, "application/json")
            .bodyValue(
                GroundedOpenAiChatRequest(
                    model = settings.text(MODEL),
                    messages = listOf(GroundedLlmMessage("system", system), GroundedLlmMessage("user", user)),
                    responseFormat = format,
                ),
            ).retrieve()
            .bodyToMono(String::class.java)
            .timeout(settings.llmTimeout())
            .block()
            ?: throw ResponseStatusException(HttpStatus.BAD_GATEWAY, "근거 검색 언어 모델 응답이 비어 있습니다.")
    }

    private fun text(body: String): String =
        objectMapper.readTree(body).path("choices").path(0).path("message").path("content").asText()

    private fun ready(): Boolean = settings.text(ENABLED).toBoolean() && settings.text(API_KEY).isNotBlank()

    private companion object {
        private const val ENABLED = "app.llm.openai.enabled"
        private const val API_KEY = "app.llm.openai.api-key"
        private const val BASE_URL = "app.llm.openai.base-url"
        private const val MODEL = "app.llm.openai.model"
    }
}
