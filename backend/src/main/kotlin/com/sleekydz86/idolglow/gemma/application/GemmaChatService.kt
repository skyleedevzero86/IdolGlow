package com.sleekydz86.idolglow.gemma.application

import tools.jackson.databind.JsonNode
import com.sleekydz86.idolglow.gemma.application.dto.GemmaChatCommand
import com.sleekydz86.idolglow.gemma.application.dto.GemmaChatResult
import com.sleekydz86.idolglow.gemma.infrastructure.GemmaApiClient
import com.sleekydz86.idolglow.global.config.GemmaProperties
import com.sleekydz86.idolglow.global.exceptions.CustomException
import com.sleekydz86.idolglow.global.exceptions.gemma.GemmaExceptionType
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class GemmaChatService(
    private val gemmaProperties: GemmaProperties,
    private val gemmaApiClient: GemmaApiClient,
) {

    private val log = LoggerFactory.getLogger(this::class.java)

    fun chat(command: GemmaChatCommand): GemmaChatResult {
        require(command.prompt.trim().isNotEmpty()) { "prompt 는 비어 있을 수 없습니다." }
        require(command.temperature in 0.0..2.0) { "temperature 는 0.0 이상 2.0 이하여야 합니다." }
        require(command.topP in 0.0..1.0) { "topP 는 0.0 이상 1.0 이하여야 합니다." }
        require(command.maxTokens in 1..2048) { "maxTokens 는 1 이상 2048 이하여야 합니다." }

        if (!gemmaProperties.enabled) {
            throw CustomException(GemmaExceptionType.GEMMA_DISABLED)
        }

        val model = command.model?.trim().takeUnless { it.isNullOrBlank() }
            ?: gemmaProperties.model.trim().ifBlank { "google/gemma-4-E4B-it" }

        val systemPrompt = buildSystemPrompt(command.systemPrompt, command.enableThinking)
        val response = gemmaApiClient.chat(
            model = model,
            systemPrompt = systemPrompt,
            userPrompt = command.prompt.trim(),
            imageUrl = command.imageUrl?.trim().takeUnless { it.isNullOrBlank() },
            temperature = command.temperature,
            topP = command.topP,
            maxTokens = command.maxTokens,
        )

        if (!response.isSuccess2xx || response.json == null) {
            log.warn(
                "Gemma 호출 실패: status={} body={} error={}",
                response.httpStatus,
                response.rawBody,
                response.error?.message,
            )
            throw CustomException(GemmaExceptionType.GEMMA_PROVIDER_UNAVAILABLE)
        }

        val answer = extractAssistantText(response.json)
        if (answer.isBlank()) {
            log.warn("Gemma 응답 본문이 비어 있습니다: {}", response.rawBody)
            throw CustomException(GemmaExceptionType.GEMMA_EMPTY_RESPONSE)
        }

        val usage = response.json.path("usage")
        return GemmaChatResult(
            model = response.json.path("model").asText(model),
            answer = answer,
            finishReason = response.json.path("choices").path(0).path("finish_reason").asText(null),
            promptTokens = usage.intOrNull("prompt_tokens"),
            completionTokens = usage.intOrNull("completion_tokens"),
            totalTokens = usage.intOrNull("total_tokens"),
            thinkingEnabled = command.enableThinking,
        )
    }

    private fun buildSystemPrompt(systemPrompt: String?, enableThinking: Boolean): String {
        val base = systemPrompt?.trim().takeUnless { it.isNullOrBlank() } ?: DEFAULT_SYSTEM_PROMPT
        if (!enableThinking || base.startsWith(THINKING_TOKEN)) {
            return base
        }
        return "$THINKING_TOKEN\n$base"
    }

    private fun extractAssistantText(root: JsonNode): String {
        val content = root.path("choices").path(0).path("message").path("content")
        return when {
            content.isMissingNode || content.isNull -> ""
            content.isTextual -> content.asText()
            content.isArray -> content.mapNotNull { part ->
                when {
                    part.path("type").asText() == "text" -> part.path("text").asText(null)
                    part.isTextual -> part.asText()
                    else -> null
                }
            }.joinToString("\n").trim()
            else -> content.toString()
        }
    }

    private fun JsonNode.intOrNull(field: String): Int? {
        val node = path(field)
        return if (node.isInt || node.isLong) node.asInt() else null
    }

    companion object {
        private const val THINKING_TOKEN = "<|think|>"
        private const val DEFAULT_SYSTEM_PROMPT =
            "You are a helpful assistant for the IdolGlow backend. Answer clearly in Korean."
    }
}
