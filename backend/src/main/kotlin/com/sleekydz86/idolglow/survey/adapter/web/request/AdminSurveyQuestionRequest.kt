package com.sleekydz86.idolglow.survey.ui.request

import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size

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
