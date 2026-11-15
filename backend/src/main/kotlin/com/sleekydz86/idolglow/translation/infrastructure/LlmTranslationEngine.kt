package com.sleekydz86.idolglow.translation.infrastructure

import com.sleekydz86.idolglow.global.llm.LlmGateway
import com.sleekydz86.idolglow.global.llm.LlmRequest
import com.sleekydz86.idolglow.translation.application.TranslationPrompt
import com.sleekydz86.idolglow.translation.application.port.out.EngineRequest
import com.sleekydz86.idolglow.translation.application.port.out.EngineResponse
import com.sleekydz86.idolglow.translation.application.port.out.JudgeRequest
import com.sleekydz86.idolglow.translation.application.port.out.TranslationEnginePort
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class LlmTranslationEngine(
    private val gateway: LlmGateway,
    private val objectMapper: ObjectMapper,
) : TranslationEnginePort {
    override fun available(): Boolean = gateway.isAvailable(TranslationPrompt.FEATURE)

    override fun translate(request: EngineRequest): EngineResponse? {
        val result =
            gateway.complete(
                LlmRequest(
                    feature = TranslationPrompt.FEATURE,
                    systemPrompt = TranslationPrompt.system(request.locale, request.glossary),
                    userPrompt = objectMapper.writeValueAsString(request.fields),
                    jsonOutput = true,
                    maxOutputTokens = outputBudget(request.fields),
                    temperature = TRANSLATE_TEMPERATURE,
                ),
            ) ?: return null
        return EngineResponse(TranslationPrompt.parseFields(result.text, objectMapper), result.provider)
    }

    override fun judge(request: JudgeRequest): Int? {
        val payload = mapOf("source" to request.source, "translation" to request.translated)
        val result =
            gateway.takeIf { it.isAvailable(TranslationPrompt.JUDGE_FEATURE) }?.complete(
                LlmRequest(
                    feature = TranslationPrompt.JUDGE_FEATURE,
                    systemPrompt = TranslationPrompt.judgeSystem(request.locale),
                    userPrompt = objectMapper.writeValueAsString(payload),
                    jsonOutput = true,
                    maxOutputTokens = JUDGE_TOKENS,
                    temperature = 0.0,
                ),
            ) ?: return null
        return TranslationPrompt.parseScore(result.text, objectMapper)
    }

    private fun outputBudget(fields: Map<String, String>): Int =
        (fields.values.sumOf { it.length } * TOKENS_PER_CHAR + BASE_TOKENS).coerceIn(BASE_TOKENS, MAX_TOKENS)

    companion object {
        private const val TRANSLATE_TEMPERATURE = 0.1
        private const val TOKENS_PER_CHAR = 3
        private const val BASE_TOKENS = 512
        private const val MAX_TOKENS = 16000
        private const val JUDGE_TOKENS = 256
    }
}
