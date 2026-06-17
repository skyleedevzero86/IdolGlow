package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyFormPrimaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormSecondaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormStatus
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import java.time.LocalDateTime

data class SurveyFormSummaryResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val descriptionTags: List<String>,
    val active: Boolean,
    val status: SurveyFormStatus,
    val statusLabel: String,
    val primaryCategory: SurveyFormPrimaryCategory,
    val primaryCategoryLabel: String,
    val secondaryCategory: SurveyFormSecondaryCategory?,
    val secondaryCategoryLabel: String?,
    val questionCount: Int,
    val requiredQuestionCount: Int,
    val choiceQuestionCount: Int,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?,
) {
    companion object {
        fun from(form: SurveyForm): SurveyFormSummaryResponse {
            val questions = form.questions
            return SurveyFormSummaryResponse(
                id = form.id,
                title = form.title,
                description = form.descriptionMarkdown(),
                descriptionTags = form.descriptionTagNames(),
                active = form.active,
                status = form.status,
                statusLabel = form.status.label,
                primaryCategory = form.primaryCategory,
                primaryCategoryLabel = form.primaryCategory.label,
                secondaryCategory = form.secondaryCategory,
                secondaryCategoryLabel = form.secondaryCategory?.label,
                questionCount = questions.size,
                requiredQuestionCount = questions.count { it.required },
                choiceQuestionCount = questions.count { it.questionType != SurveyQuestionType.TEXT },
                createdAt = form.createdAt,
                updatedAt = form.updatedAt,
            )
        }
    }
}
