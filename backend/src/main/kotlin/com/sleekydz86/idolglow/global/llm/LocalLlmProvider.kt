package com.sleekydz86.idolglow.global.llm

import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient
import tools.jackson.databind.ObjectMapper
import java.time.Duration

@Component
class LocalLlmProvider(
    private val webClient: WebClient,
    private val objectMapper: ObjectMapper,
    private val props: LocalLlmProperties,
) : LlmProvider {
    override val name = "local"
    override val external = false

    override fun isConfigured(): Boolean = props.enabled && props.baseUrl.isNotBlank() && props.model.isNotBlank()

    override fun complete(request: LlmRequest): String? {
        val payload = OpenAiCompatiblePayload.build(objectMapper, props.model, request)
        val raw =
            webClient
                .post()
                .uri("${props.baseUrl.trimEnd('/')}/chat/completions")
                .header(HttpHeaders.CONTENT_TYPE, "application/json")
                .bodyValue(objectMapper.writeValueAsString(payload))
                .retrieve()
                .bodyToMono(String::class.java)
                .block(Duration.ofSeconds(props.timeoutSeconds))
                ?: return null
        return OpenAiCompatiblePayload.firstMessageContent(objectMapper, raw)
    }
}
