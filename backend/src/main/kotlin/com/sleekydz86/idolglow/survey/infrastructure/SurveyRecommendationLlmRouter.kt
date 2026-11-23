package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.global.llm.LlmGateway
import com.sleekydz86.idolglow.global.llm.LlmRequest
import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class SurveyRecommendationLlmRouter(
    private val llmGateway: LlmGateway,
    private val objectMapper: ObjectMapper,
    private val validator: SurveyRecommendationValidator = SurveyRecommendationValidator(objectMapper),
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun generate(
        answerHighlights: List<String>,
        attractions: List<TourAttraction>,
        reasonSummaries: List<String> = emptyList(),
    ): LlmSurveyRecommendation? {
        val result =
            llmGateway.complete(
                LlmRequest(
                    feature = FEATURE,
                    systemPrompt = SurveyLlmPrompts.system,
                    userPrompt =
                        SurveyLlmPrompts.userJson(objectMapper, answerHighlights, attractions, reasonSummaries),
                    maxOutputTokens = MAX_OUTPUT_TOKENS,
                    temperature = TEMPERATURE,
                ),
            ) ?: return null
        val source = (answerHighlights + attractions.map { it.name } + reasonSummaries).joinToString(" ").lowercase()
        val codes = attractions.map { it.attractionCode }.toSet()
        return validator.parse(result.text)
            ?.let { it.copy(attractionReasons = it.attractionReasons.filterKeys(codes::contains)) }
            ?.takeIf { validator.grounded(it, source) }
            .also { if (it == null) logDropped(result.provider, result.text) }
    }

    private fun logDropped(
        provider: String,
        text: String,
    ) {
        log.warn("설문 추천 결과 버림 provider={} text={}", provider, text.take(LOG_TEXT_LIMIT))
    }

    companion object {
        private const val FEATURE: String = "survey"
        private const val MAX_OUTPUT_TOKENS: Int = 900
        private const val TEMPERATURE: Double = 0.3
        private const val LOG_TEXT_LIMIT: Int = 200
    }
}
