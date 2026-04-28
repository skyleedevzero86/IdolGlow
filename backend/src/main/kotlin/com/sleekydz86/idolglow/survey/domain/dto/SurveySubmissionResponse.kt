package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveySubmission
import java.time.LocalDateTime

data class SurveySubmissionResponse(
    val id: Long,
    val formId: Long,
    val submittedAt: LocalDateTime?,
    val answers: List<SurveySubmittedAnswerResponse>,
) {
    companion object {
        fun from(submission: SurveySubmission): SurveySubmissionResponse =
            SurveySubmissionResponse(
                id = submission.id,
                formId = submission.form.id,
                submittedAt = submission.createdAt,
                answers = submission.answers.map { answer ->
                    SurveySubmittedAnswerResponse(
                        questionId = answer.question.id,
                        answerText = answer.answerText,
                        selectedOptions = answer.selectedOptions
                            .sortedBy { it.displayOrder }
                            .map { it.optionText },
                    )
                },
            )
    }
}

data class SurveySubmittedAnswerResponse(
    val questionId: Long,
    val answerText: String?,
    val selectedOptions: List<String>,
)
