package com.sleekydz86.idolglow.survey.application.dto

import com.sleekydz86.idolglow.survey.domain.SurveyFormPrimaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormSecondaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormStatus
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType

data class UpsertSurveyFormCommand(
    val title: String,
    val description: String?,
    val descriptionTags: List<String>,
    val status: SurveyFormStatus,
    val primaryCategory: SurveyFormPrimaryCategory,
    val secondaryCategory: SurveyFormSecondaryCategory?,
    val questions: List<UpsertSurveyQuestionCommand>,
)

data class UpsertSurveyQuestionCommand(
    val order: Int,
    val title: String,
    val description: String?,
    val type: SurveyQuestionType,
    val required: Boolean,
    val options: List<String>,
)

data class SubmitSurveyResponseCommand(
    val answers: List<SubmitSurveyAnswerCommand>,
)

data class SubmitSurveyAnswerCommand(
    val questionId: Long,
    val answerText: String?,
    val selectedOptions: List<String>,
)
