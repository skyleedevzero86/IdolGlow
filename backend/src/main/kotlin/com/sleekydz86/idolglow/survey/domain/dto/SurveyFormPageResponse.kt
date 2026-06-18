package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveyForm
import org.springframework.data.domain.Page

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
