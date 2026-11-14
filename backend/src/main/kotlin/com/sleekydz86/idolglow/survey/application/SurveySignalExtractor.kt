package com.sleekydz86.idolglow.survey.application

import com.sleekydz86.idolglow.productpackage.discovery.application.ConceptAliases
import com.sleekydz86.idolglow.productpackage.discovery.application.RecommendationSignals
import com.sleekydz86.idolglow.survey.domain.RecommendationSignalType
import com.sleekydz86.idolglow.survey.domain.SurveyAnswer
import com.sleekydz86.idolglow.survey.domain.SurveySubmission

object SurveySignalExtractor {
    private val listSeparators = Regex("[,\\n]")

    fun extract(submission: SurveySubmission): RecommendationSignals {
        val textsBySignal =
            submission.answers
                .mapNotNull { answer -> answer.question.recommendationSignal?.let { it to answer } }
                .groupBy({ it.first }, { it.second })

        fun optionTexts(type: RecommendationSignalType): List<String> {
            val answers = textsBySignal[type].orEmpty()
            return answers.flatMap { it.selectedTexts() }
        }

        fun allTexts(type: RecommendationSignalType): List<String> {
            val answers = textsBySignal[type].orEmpty()
            return answers.flatMap { listOfNotNull(it.writtenText()) + it.selectedTexts() }
        }

        return RecommendationSignals(
            conceptTags =
                allTexts(RecommendationSignalType.CONCEPT)
                    .mapNotNull { ConceptAliases.resolve(it)?.name }
                    .toSet(),
            idolTokens = RecommendationSignals.idolTokens(allTexts(RecommendationSignalType.IDOL).joinToString(" ")),
            placeKeywords =
                RecommendationSignals.keywords(
                    allTexts(RecommendationSignalType.PLACE).flatMap { it.split(listSeparators) },
                ),
            extraTags = optionTexts(RecommendationSignalType.TAG).toSet(),
        )
    }

    private fun SurveyAnswer.writtenText(): String? = answerText?.trim()?.takeIf { it.isNotEmpty() }

    private fun SurveyAnswer.selectedTexts(): List<String> =
        selectedOptions
            .sortedBy { it.displayOrder }
            .map { it.optionText.trim() }
            .filter { it.isNotEmpty() }
}
