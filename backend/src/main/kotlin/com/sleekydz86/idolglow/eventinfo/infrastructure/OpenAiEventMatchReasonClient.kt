package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Component
class OpenAiEventMatchReasonClient(
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
    private val log = LoggerFactory.getLogger(javaClass)

    fun reason(prompt: String): String? {
        if (!ready()) {
            return null
        }
        return read(post(prompt))
    }

    private fun ready(): Boolean = enabled && apiKey.isNotBlank()

    private fun post(prompt: String): String? =
        runCatching {
            webClient
                .post()
                .uri("${baseUrl.trimEnd('/')}/v1/chat/completions")
                .header(HttpHeaders.AUTHORIZATION, "Bearer ${apiKey.trim()}")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(body(prompt))
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
        }.onFailure { log.warn("공연 매칭 OpenAI 근거 호출 실패 type={}", it.javaClass.simpleName) }.getOrNull()

    private fun body(prompt: String): EventMatchOpenAiRequest =
        EventMatchOpenAiRequest(
            model = model,
            temperature = 0.toDouble(),
            messages =
                listOf(
                    EventMatchChatMessage(role = "system", content = SYSTEM),
                    EventMatchChatMessage(role = "user", content = prompt),
                ),
        )

    private fun read(body: String?): String? =
        body?.let {
            runCatching {
                objectMapper
                    .readValue(it, EventMatchOpenAiResponse::class.java)
                    .choices
                    .firstOrNull()
                    ?.message
                    ?.content
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            }.getOrNull()
        }

    companion object {
        private const val SYSTEM: String = "공연 두 건이 같은지 한 문장으로만 답한다. 키, 토큰, 개인정보를 쓰지 않는다."
    }
}

data class EventMatchOpenAiRequest(
    val model: String,
    val temperature: Double,
    val messages: List<EventMatchChatMessage>,
)

data class EventMatchChatMessage(
    val role: String,
    val content: String,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventMatchOpenAiResponse(
    val choices: List<EventMatchOpenAiChoice> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventMatchOpenAiChoice(
    val message: EventMatchChatMessage = EventMatchChatMessage(role = "", content = ""),
)
