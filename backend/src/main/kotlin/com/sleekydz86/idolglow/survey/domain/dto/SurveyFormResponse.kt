package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyFormPrimaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormSecondaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormStatus

data class SurveyFormResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val descriptionTags: List<String>,
    val status: SurveyFormStatus,
    val statusLabel: String,
    val primaryCategory: SurveyFormPrimaryCategory,
    val primaryCategoryLabel: String,
    val secondaryCategory: SurveyFormSecondaryCategory?,
    val secondaryCategoryLabel: String?,
    val questions: List<SurveyQuestionResponse>,
) {
    companion object {
        fun from(form: SurveyForm): SurveyFormResponse =
            SurveyFormResponse(
                id = form.id,
                title = form.title,
                description = form.descriptionMarkdown(),
                descriptionTags = form.descriptionTagNames(),
                status = form.status,
                statusLabel = form.status.label,
                primaryCategory = form.primaryCategory,
                primaryCategoryLabel = form.primaryCategory.label,
                secondaryCategory = form.secondaryCategory,
                secondaryCategoryLabel = form.secondaryCategory?.label,
                questions =
                    form.questions
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
