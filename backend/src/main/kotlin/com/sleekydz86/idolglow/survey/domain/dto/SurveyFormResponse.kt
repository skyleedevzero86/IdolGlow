package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveyForm
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import org.springframework.data.domain.Page
import java.time.LocalDateTime

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

data class SurveyFormSummaryResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val active: Boolean,
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
                description = form.description,
                active = form.active,
                questionCount = questions.size,
                requiredQuestionCount = questions.count { it.required },
                choiceQuestionCount = questions.count { it.questionType != SurveyQuestionType.TEXT },
                createdAt = form.createdAt,
                updatedAt = form.updatedAt,
            )
        }
    }
}

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
