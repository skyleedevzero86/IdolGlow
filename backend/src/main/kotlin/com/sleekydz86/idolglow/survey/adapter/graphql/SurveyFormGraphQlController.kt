package com.sleekydz86.idolglow.survey.graphql

import com.sleekydz86.idolglow.global.adapter.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.survey.application.AdminSurveyFormService
import com.sleekydz86.idolglow.survey.application.UserSurveyFormService
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

@Controller
class SurveyFormGraphQlController(
    private val adminSurveyFormService: AdminSurveyFormService,
    private val userSurveyFormService: UserSurveyFormService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {
    @QueryMapping
    fun currentSurveyForm(): SurveyFormGraphQlResponse? =
        userSurveyFormService.findCurrentForm()?.let(SurveyFormGraphQlResponse::from)

    @QueryMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun adminCurrentSurveyForm(): SurveyFormGraphQlResponse? =
        adminSurveyFormService.findCurrent()?.let(SurveyFormGraphQlResponse::from)

    @QueryMapping
    @PreAuthorize("isAuthenticated()")
    fun myLatestSurveySubmission(): SurveySubmissionGraphQlResponse? =
        userSurveyFormService.findMyLatestSubmission(authenticatedUserIdResolver.resolveRequired())
            ?.let(SurveySubmissionGraphQlResponse::from)

    @MutationMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun upsertAdminCurrentSurveyForm(@Argument input: UpsertAdminSurveyFormGraphQlInput): SurveyFormGraphQlResponse {
        val response = adminSurveyFormService.upsertCurrent(
            AdminUpsertSurveyFormRequest(
                title = input.title,
                description = input.description,
                questions = input.questions.map {
                    AdminSurveyQuestionRequest(
                        order = it.order,
                        title = it.title,
                        description = it.description,
                        type = it.type,
                        required = it.required,
                        options = it.options,
                    )
                },
            ),
        )
        return SurveyFormGraphQlResponse.from(response)
    }

    @MutationMapping
    @PreAuthorize("isAuthenticated()")
    fun submitCurrentSurveyForm(@Argument input: SubmitSurveyFormGraphQlInput): SurveySubmissionGraphQlResponse {
        val response = userSurveyFormService.submitCurrentForm(
            authenticatedUserIdResolver.resolveRequired(),
            SubmitSurveyResponseRequest(
                answers = input.answers.map {
                    SurveyAnswerRequest(
                        questionId = it.questionId,
                        answerText = it.answerText,
                        selectedOptions = it.selectedOptions,
                    )
                },
            ),
        )
        return SurveySubmissionGraphQlResponse.from(response)
    }
}

data class UpsertAdminSurveyFormGraphQlInput(
    val title: String,
    val description: String?,
    val questions: List<UpsertAdminSurveyQuestionGraphQlInput>,
)

data class UpsertAdminSurveyQuestionGraphQlInput(
    val order: Int,
    val title: String,
    val description: String?,
    val type: SurveyQuestionType,
    val required: Boolean = false,
    val options: List<String> = emptyList(),
)

data class SubmitSurveyFormGraphQlInput(
    val answers: List<SubmitSurveyAnswerGraphQlInput>,
)

data class SubmitSurveyAnswerGraphQlInput(
    val questionId: Long,
    val answerText: String?,
    val selectedOptions: List<String> = emptyList(),
)

data class SurveyFormGraphQlResponse(
    val id: Long,
    val title: String,
    val description: String?,
    val questions: List<SurveyQuestionGraphQlResponse>,
) {
    companion object {
        fun from(source: SurveyFormResponse): SurveyFormGraphQlResponse =
            SurveyFormGraphQlResponse(
                id = source.id,
                title = source.title,
                description = source.description,
                questions = source.questions.map {
                    SurveyQuestionGraphQlResponse(
                        id = it.id,
                        order = it.order,
                        title = it.title,
                        description = it.description,
                        type = it.type,
                        required = it.required,
                        options = it.options,
                    )
                },
            )
    }
}

data class SurveyQuestionGraphQlResponse(
    val id: Long,
    val order: Int,
    val title: String,
    val description: String?,
    val type: SurveyQuestionType,
    val required: Boolean,
    val options: List<String>,
)

data class SurveySubmissionGraphQlResponse(
    val id: Long,
    val formId: Long,
    val submittedAt: String?,
    val answers: List<SurveySubmittedAnswerGraphQlResponse>,
) {
    companion object {
        fun from(source: SurveySubmissionResponse): SurveySubmissionGraphQlResponse =
            SurveySubmissionGraphQlResponse(
                id = source.id,
                formId = source.formId,
                submittedAt = source.submittedAt?.toString(),
                answers = source.answers.map {
                    SurveySubmittedAnswerGraphQlResponse(
                        questionId = it.questionId,
                        answerText = it.answerText,
                        selectedOptions = it.selectedOptions,
                    )
                },
            )
    }
}

data class SurveySubmittedAnswerGraphQlResponse(
    val questionId: Long,
    val answerText: String?,
    val selectedOptions: List<String>,
)
