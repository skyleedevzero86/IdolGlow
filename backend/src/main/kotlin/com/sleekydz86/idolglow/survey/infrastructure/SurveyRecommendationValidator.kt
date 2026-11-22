package com.sleekydz86.idolglow.survey.infrastructure

import org.springframework.stereotype.Component
import tools.jackson.databind.ObjectMapper

@Component
class SurveyRecommendationValidator(
    private val objectMapper: ObjectMapper,
) {
    fun parse(text: String): LlmSurveyRecommendation? {
        val json =
            text
                .trim()
                .removePrefix("```json")
                .removePrefix("```")
                .removeSuffix("```")
                .trim()
        return runCatching { objectMapper.readValue(json, LlmSurveyRecommendation::class.java).normalize() }.getOrNull()
    }

    fun grounded(
        recommendation: LlmSurveyRecommendation,
        source: String,
    ): Boolean {
        val title = recommendation.title
        val texts =
            listOfNotNull(title, recommendation.subtitle, recommendation.narrative) +
                recommendation.attractionReasons.values
        return title != null &&
            title.length <= MAX_TITLE_LENGTH &&
            recommendation.narrative != null &&
            texts.none { FOREIGN_SCRIPT.containsMatchIn(it) } &&
            texts.all { text -> LATIN_WORD.findAll(text).all { source.contains(it.value.lowercase()) } }
    }

    companion object {
        private const val MAX_TITLE_LENGTH: Int = 40
        private val FOREIGN_SCRIPT = Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsCyrillic}]")
        private val LATIN_WORD = Regex("[A-Za-z]{2,}")
    }
}
