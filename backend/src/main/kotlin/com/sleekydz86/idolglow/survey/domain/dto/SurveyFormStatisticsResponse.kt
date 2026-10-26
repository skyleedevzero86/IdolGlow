package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveyAnswer
import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyQuestion
import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import kotlin.math.roundToInt

data class SurveyFormStatisticsResponse(
    val formId: Long,
    val title: String,
    val participantCount: Int,
    val questions: List<SurveyQuestionStatisticsResponse>,
) {
    companion object {
        fun from(
            form: SurveyForm,
            submissions: List<SurveySubmission>,
        ): SurveyFormStatisticsResponse =
            SurveyFormStatisticsResponse(
                formId = form.id,
                title = form.title,
                participantCount = submissions.size,
                questions = form.questions.sortedBy { it.displayOrder }.map { questionStatistics(it, submissions) },
            )

        private fun questionStatistics(
            question: SurveyQuestion,
            submissions: List<SurveySubmission>,
        ): SurveyQuestionStatisticsResponse {
            val answers =
                submissions.mapNotNull { submission ->
                    submission.answers.firstOrNull { it.question.id == question.id }
                }
            val responseCount = answers.count { it.answerText?.isNotBlank() == true || it.selectedOptions.isNotEmpty() }
            if (question.questionType.isWrittenAnswer()) {
                return writtenQuestion(question, responseCount)
            }
            return choiceQuestion(question, answers, responseCount)
        }

        private fun writtenQuestion(
            question: SurveyQuestion,
            responseCount: Int,
        ): SurveyQuestionStatisticsResponse =
            SurveyQuestionStatisticsResponse(
                questionId = question.id,
                order = question.displayOrder,
                title = question.title,
                required = question.required,
                written = true,
                responseCount = responseCount,
                options = emptyList(),
            )

        private fun choiceQuestion(
            question: SurveyQuestion,
            answers: List<SurveyAnswer>,
            responseCount: Int,
        ): SurveyQuestionStatisticsResponse {
            val counts = answers.flatMap { it.selectedOptions }.groupingBy { it.optionText }.eachCount()
            val options =
                question.options.sortedBy { it.displayOrder }.map { option ->
                    val count = counts[option.optionText] ?: 0
                    SurveyOptionStatisticsResponse(
                        label = option.optionText,
                        count = count,
                        percent = percent(count, responseCount),
                    )
                }
            return SurveyQuestionStatisticsResponse(
                questionId = question.id,
                order = question.displayOrder,
                title = question.title,
                required = question.required,
                written = false,
                responseCount = responseCount,
                options = options,
            )
        }

        private const val PERCENT_SCALE = 100.0

        private fun percent(
            count: Int,
            total: Int,
        ): Int = if (total <= 0) 0 else ((count * PERCENT_SCALE) / total).roundToInt()
    }
}

data class SurveyQuestionStatisticsResponse(
    val questionId: Long,
    val order: Int,
    val title: String,
    val required: Boolean,
    val written: Boolean,
    val responseCount: Int,
    val options: List<SurveyOptionStatisticsResponse>,
)

data class SurveyOptionStatisticsResponse(
    val label: String,
    val count: Int,
    val percent: Int,
)
