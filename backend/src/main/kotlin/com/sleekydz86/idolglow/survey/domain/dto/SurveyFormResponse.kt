package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType

data class SurveyFormResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val questions: List<SurveyQuestionResponse>,
) {
    companion object {
        fun from(form: SurveyForm): SurveyFormResponse =
            SurveyFormResponse(
                id = form.id,
                title = form.title,
                description = form.description,
                questions = form.questions
                    .sortedBy { it.displayOrder }
                    .map { q ->
                        SurveyQuestionResponse(
                            id = q.id,
                            order = q.displayOrder,
                            title = q.title,
                            description = q.description,
                            type = q.questionType,
                            required = q.required,
                            options = q.options.sortedBy { it.displayOrder }.map { it.optionText },
                        )
                    },
            )
    }
}

data class SurveyQuestionResponse(
    val id: Long,
    val order: Int,
    val title: String,
    val description: String?,
    val type: SurveyQuestionType,
    val required: Boolean,
    val options: List<String>,
)
