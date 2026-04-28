package com.sleekydz86.idolglow.survey.infrastructure

import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class SurveyRecommendationLlmRouter(
    private val openAiSurveyRecommendationClient: OpenAiSurveyRecommendationClient,
    private val anthropicSurveyRecommendationClient: AnthropicSurveyRecommendationClient,
    @Value("\${app.llm.survey.provider:auto}")
    private val providerRaw: String,
) {

    fun generate(
        titleFallback: String,
        subtitleFallback: String,
        narrativeFallback: String,
        answerHighlights: List<String>,
        attractions: List<TourAttraction>,
    ): LlmSurveyRecommendation? {
        val p = providerRaw.trim().lowercase()
        return when (p) {
            "openai", "gpt" ->
                openAiSurveyRecommendationClient.generate(
                    titleFallback,
                    subtitleFallback,
                    narrativeFallback,
                    answerHighlights,
                    attractions,
                )
            "anthropic", "claude" ->
                anthropicSurveyRecommendationClient.generate(
                    titleFallback,
                    subtitleFallback,
                    narrativeFallback,
                    answerHighlights,
                    attractions,
                )
            else ->
                openAiSurveyRecommendationClient.generate(
                    titleFallback,
                    subtitleFallback,
                    narrativeFallback,
                    answerHighlights,
                    attractions,
                )
                    ?: anthropicSurveyRecommendationClient.generate(
                        titleFallback,
                        subtitleFallback,
                        narrativeFallback,
                        answerHighlights,
                        attractions,
                    )
        }
    }
}
