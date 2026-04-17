package com.sleekydz86.idolglow.gemma.infrastructure

import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import com.sleekydz86.idolglow.global.config.GemmaProperties
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.body

@Component
class GemmaApiClient(
    private val props: GemmaProperties,
    @param:Qualifier("gemmaRestClient") private val restClient: RestClient,
    private val objectMapper: ObjectMapper,
) {

    fun chat(
        model: String,
        systemPrompt: String,
        userPrompt: String,
        imageUrl: String?,
        temperature: Double,
        topP: Double,
        maxTokens: Int,
    ): GemmaApiResponse {
        val payload = objectMapper.createObjectNode().apply {
            put("model", model)
            put("temperature", temperature)
            put("top_p", topP)
            put("max_tokens", maxTokens)
            set("messages", buildMessages(systemPrompt, userPrompt, imageUrl))
        }

        return try {
            val entity = restClient.post()
                .uri("/chat/completions")
                .headers { headers ->
                    headers.contentType = MediaType.APPLICATION_JSON
                    if (props.apiKey.isNotBlank()) {
                        headers.setBearerAuth(props.apiKey.trim())
                    }
                }
                .body(objectMapper.writeValueAsString(payload))
                .retrieve()
                .toEntity(String::class.java)

            val raw = entity.body ?: ""
            val json = runCatching { objectMapper.readTree(raw) }.getOrNull()
            GemmaApiResponse(
                httpStatus = entity.statusCode.value(),
                json = json,
                rawBody = raw,
                error = null,
            )
        } catch (e: Exception) {
            GemmaApiResponse(
                httpStatus = 0,
                json = null,
                rawBody = e.message,
                error = e,
            )
        }
    }

    private fun buildMessages(
        systemPrompt: String,
        userPrompt: String,
        imageUrl: String?,
    ): JsonNode {
        val messages = objectMapper.createArrayNode()

        messages.add(
            objectMapper.createObjectNode().apply {
                put("role", "system")
                put("content", systemPrompt)
            }
        )

        messages.add(
            objectMapper.createObjectNode().apply {
                put("role", "user")
                if (imageUrl.isNullOrBlank()) {
                    put("content", userPrompt)
                } else {
                    set(
                        "content",
                        objectMapper.createArrayNode().apply {
                            add(
                                objectMapper.createObjectNode().apply {
                                    put("type", "image_url")
                                    set(
                                        "image_url",
                                        objectMapper.createObjectNode().apply {
                                            put("url", imageUrl.trim())
                                        }
                                    )
                                }
                            )
                            add(
                                objectMapper.createObjectNode().apply {
                                    put("type", "text")
                                    put("text", userPrompt)
                                }
                            )
                        }
                    )
                }
            }
        )

        return messages
    }
}

data class GemmaApiResponse(
    val httpStatus: Int,
    val json: JsonNode?,
    val rawBody: String?,
    val error: Throwable? = null,
) {
    val isSuccess2xx: Boolean get() = httpStatus in 200..299
}
