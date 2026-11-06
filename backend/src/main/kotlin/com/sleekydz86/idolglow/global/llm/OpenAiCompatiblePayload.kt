package com.sleekydz86.idolglow.global.llm

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.node.ObjectNode

object OpenAiCompatiblePayload {
    fun build(
        objectMapper: ObjectMapper,
        model: String,
        request: LlmRequest,
    ): ObjectNode =
        objectMapper.createObjectNode().apply {
            put("model", model)
            put("temperature", request.temperature)
            put("max_tokens", request.maxOutputTokens)
            if (request.jsonOutput) {
                set("response_format", objectMapper.createObjectNode().put("type", "json_object"))
            }
            set(
                "messages",
                objectMapper.createArrayNode().apply {
                    add(objectMapper.createObjectNode().put("role", "system").put("content", request.systemPrompt))
                    add(objectMapper.createObjectNode().put("role", "user").put("content", request.userPrompt))
                },
            )
        }

    fun firstMessageContent(
        objectMapper: ObjectMapper,
        raw: String,
    ): String? =
        objectMapper
            .readTree(raw)
            .path("choices")
            .path(0)
            .path("message")
            .path("content")
            .asText("")
            .takeIf { it.isNotBlank() }
}
