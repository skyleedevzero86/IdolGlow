package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyFormPrimaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormSecondaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormStatus
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import org.springframework.data.domain.Page
import java.time.LocalDateTime

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

private fun SurveyForm.descriptionMarkdown(): String? =
    descriptionContent?.markdown?.takeIf { it.isNotBlank() }
        ?: description.takeIf { descriptionContent == null }

private fun SurveyForm.descriptionTagNames(): List<String> =
    descriptionContent
        ?.tags
        ?.sortedBy { it.displayOrder }
        ?.map { it.tagName }
        ?: emptyList()

data class SurveyFormPageResponse(
    val content: List<SurveyFormSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
) {
    companion object {
        fun from(page: Page<SurveyForm>): SurveyFormPageResponse =
            SurveyFormPageResponse(
                content = page.content.map(SurveyFormSummaryResponse::from),
                page = if (page.totalElements == 0L) 1 else page.number + 1,
                size = page.size,
                totalElements = page.totalElements,
                totalPages = page.totalPages.coerceAtLeast(1),
                hasNext = page.hasNext(),
            )
    }
}
