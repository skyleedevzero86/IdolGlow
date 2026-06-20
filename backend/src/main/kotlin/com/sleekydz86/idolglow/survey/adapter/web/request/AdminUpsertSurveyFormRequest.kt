package com.sleekydz86.idolglow.survey.adapter.web.request

import com.sleekydz86.idolglow.survey.domain.SurveyFormPrimaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormSecondaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class AdminUpsertSurveyFormRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:Size(max = 10000)
    val description: String? = null,
    @field:Size(max = 30)
    val descriptionTags: List<
        @Size(max = 100)
        String,
    > = emptyList(),
    @field:NotNull
    val status: SurveyFormStatus = SurveyFormStatus.SCHEDULED,
    @field:NotNull
    val primaryCategory: SurveyFormPrimaryCategory = SurveyFormPrimaryCategory.ALL,
    val secondaryCategory: SurveyFormSecondaryCategory? = null,
    @field:NotEmpty
    @field:Size(min = 1, max = 5, message = "문항은 1개 이상 5개 이하여야 합니다.")
    val questions: List<@Valid AdminSurveyQuestionRequest>,
)
