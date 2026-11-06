package com.sleekydz86.idolglow.survey.eval

import com.sleekydz86.idolglow.survey.infrastructure.SurveyRecommendationValidator

class SurveyLlmEvalScorer(
    private val validator: SurveyRecommendationValidator,
) {
    fun score(
        text: String,
        case: SurveyEvalCase,
    ): SurveyEvalScore {
        val source = (case.answerHighlights + case.attractions.map { it.name }).joinToString(" ").lowercase()
        val parsed =
            validator.parse(text)
                ?: return SurveyEvalScore(false, false, forbidden(text, source), listOf("JSON 형식 해석 실패"))
        val expectedCodes = case.attractions.map { it.attractionCode }.toSet()
        val actualCodes = parsed.attractionReasons.keys
        val missingCodes = expectedCodes - actualCodes
        val outsideCodes = actualCodes - expectedCodes
        val missingFields =
            listOf("title" to parsed.title, "subtitle" to parsed.subtitle, "narrative" to parsed.narrative)
                .filter { it.second == null }
                .map { it.first }
        val formatSuccess = missingFields.isEmpty() && missingCodes.isEmpty()
        val grounded = validator.grounded(parsed, source)
        val generatedText =
            listOfNotNull(parsed.title, parsed.subtitle, parsed.narrative) + parsed.attractionReasons.values
        val hasForbiddenExpression = generatedText.any { forbidden(it, source) }
        val reasons =
            buildList {
                if (missingFields.isNotEmpty()) add("필수 값 누락${values(missingFields.toSet())}")
                if (missingCodes.isNotEmpty()) add("관광지 이유 누락${values(missingCodes)}")
                if (outsideCodes.isNotEmpty()) add("입력에 없는 관광지 코드${values(outsideCodes)}")
                if (!grounded) add("입력 근거 검증 실패")
                if (hasForbiddenExpression) add("금지 표현 포함")
            }
        return SurveyEvalScore(
            formatSuccess,
            outsideCodes.isNotEmpty() || !grounded,
            hasForbiddenExpression,
            reasons,
        )
    }

    private fun forbidden(
        text: String,
        source: String,
    ): Boolean =
        FORBIDDEN_SCRIPT.containsMatchIn(text) ||
            PRICE.containsMatchIn(text) ||
            NUMBER.findAll(text).any { !source.contains(it.value) }

    private fun values(values: Set<String>): String {
        if (values.isEmpty()) return ""
        return values.joinToString(", ", "(", ")")
    }

    companion object {
        private val FORBIDDEN_SCRIPT = Regex("[\\p{IsHan}\\p{IsHiragana}\\p{IsKatakana}\\p{IsCyrillic}]")
        private val PRICE = Regex("[₩￦$€¥]|\\d[\\d,.]*\\s*(?:만\\s*)?(?:원|달러|엔|유로)")
        private val NUMBER = Regex("\\d+")
    }
}
