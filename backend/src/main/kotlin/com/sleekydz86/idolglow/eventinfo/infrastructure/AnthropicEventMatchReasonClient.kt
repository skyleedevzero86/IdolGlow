package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper

@Component
class AnthropicEventMatchReasonClient(
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
                .uri("${baseUrl.trimEnd('/')}/v1/messages")
                .header("x-api-key", apiKey.trim())
                .header("anthropic-version", ANTHROPIC_VERSION)
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(body(prompt))
                .retrieve()
                .bodyToMono(String::class.java)
                .block()
        }.onFailure { log.warn("공연 매칭 Anthropic 근거 호출 실패 type={}", it.javaClass.simpleName) }.getOrNull()

    private fun body(prompt: String): EventMatchAnthropicRequest =
        EventMatchAnthropicRequest(
            model = model,
            messages = listOf(EventMatchChatMessage(role = "user", content = prompt)),
            system = SYSTEM,
            temperature = 0.toDouble(),
            maxTokens = maxTokens(),
        )

    private fun read(body: String?): String? =
        body?.let {
            runCatching {
                objectMapper
                    .readValue(it, EventMatchAnthropicResponse::class.java)
                    .content
                    .firstOrNull { block -> block.type == "text" }
                    ?.text
                    ?.trim()
                    ?.takeIf(String::isNotEmpty)
            }.getOrNull()
        }

    companion object {
        private const val ANTHROPIC_VERSION: String = "2023-06-01"
        private const val SYSTEM: String = "공연 두 건이 같은지 한 문장으로만 답한다. 키, 토큰, 개인정보를 쓰지 않는다."

        private fun maxTokens(): Int = (2 shl 2) * (2 shl 2) * (2 shl 1)
    }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventMatchAnthropicRequest(
    val model: String,
    val messages: List<EventMatchChatMessage>,
    val system: String,
    val temperature: Double,
    @JsonProperty("max_tokens")
    val maxTokens: Int,
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventMatchAnthropicResponse(
    val content: List<EventMatchAnthropicBlock> = emptyList(),
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class EventMatchAnthropicBlock(
    val type: String? = null,
    val text: String? = null,
)
