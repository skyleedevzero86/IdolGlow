package com.sleekydz86.idolglow.survey.ui.request

import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

data class AdminUpsertSurveyFormRequest(
    @field:NotBlank
    @field:Size(max = 200)
    val title: String,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:NotEmpty
    @field:Size(min = 1, max = 5, message = "문항은 1개 이상 5개 이하여야 합니다.")
    val questions: List<@Valid AdminSurveyQuestionRequest>,
)

data class AdminSurveyQuestionRequest(
    @field:NotNull
    val order: Int,
    @field:NotBlank
    @field:Size(max = 300)
    val title: String,
    @field:Size(max = 2000)
    val description: String? = null,
    @field:NotNull
    val type: SurveyQuestionType,
    val required: Boolean = false,
    val options: List<@NotBlank @Size(max = 300) String> = emptyList(),
)
