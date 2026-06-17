package com.sleekydz86.idolglow.survey.graphql

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.survey.application.AdminSurveyFormService
import com.sleekydz86.idolglow.survey.application.UserSurveyFormService
import com.sleekydz86.idolglow.survey.domain.SurveyFormPrimaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormSecondaryCategory
import com.sleekydz86.idolglow.survey.domain.SurveyFormStatus
import com.sleekydz86.idolglow.survey.domain.SurveyQuestionType
import com.sleekydz86.idolglow.survey.domain.dto.SurveyFormResponse
import com.sleekydz86.idolglow.survey.domain.dto.SurveySubmissionResponse
import com.sleekydz86.idolglow.survey.ui.request.AdminSurveyQuestionRequest
import com.sleekydz86.idolglow.survey.ui.request.AdminUpsertSurveyFormRequest
import com.sleekydz86.idolglow.survey.ui.request.SubmitSurveyResponseRequest
import com.sleekydz86.idolglow.survey.ui.request.SurveyAnswerRequest
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

data class SurveySubmittedAnswerGraphQlResponse(
    val questionId: Long,
    val answerText: String?,
    val selectedOptions: List<String>,
)
