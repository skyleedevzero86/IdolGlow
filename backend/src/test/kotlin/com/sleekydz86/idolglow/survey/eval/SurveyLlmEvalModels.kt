package com.sleekydz86.idolglow.survey.eval

import com.sleekydz86.idolglow.productpackage.attraction.domain.TourAttraction

data class SurveyEvalAttraction(
    val attractionCode: String,
    val name: String,
    val signguName: String?,
    val categoryMiddle: String?,
) {
    fun domain(): TourAttraction =
        TourAttraction(
            attractionCode,
            name,
            AREA_CODE,
            AREA_NAME,
            SIGNGU_CODE,
            signguName,
            CATEGORY_LARGE,
            categoryMiddle,
            RANK,
            null,
            null,
            BASE_YM,
        )

    companion object {
        private const val AREA_CODE: Int = 1
        private const val AREA_NAME: String = "서울"
        private const val SIGNGU_CODE: Int = 0
        private const val CATEGORY_LARGE: String = "관광지"
        private const val RANK: Int = 1
        private const val BASE_YM: String = "202610"
    }
}

data class SurveyEvalCase(
    val answerHighlights: List<String>,
    val attractions: List<SurveyEvalAttraction>,
)

data class SurveyEvalScore(
    val formatSuccess: Boolean,
    val outsideRecommendation: Boolean,
    val forbiddenExpression: Boolean,
    val reasons: List<String>,
)

data class SurveyEvalSampleResult(
    val sampleNumber: Int,
    val latencyMs: Long,
    val score: SurveyEvalScore,
    val responseText: String? = null,
)

data class SurveyEvalProviderResult(
    val provider: String,
    val samples: List<SurveyEvalSampleResult>,
    val skippedReason: String? = null,
    val cost: String,
)
