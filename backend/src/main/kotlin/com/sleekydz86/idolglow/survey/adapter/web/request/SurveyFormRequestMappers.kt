package com.sleekydz86.idolglow.survey.adapter.web.request

import com.sleekydz86.idolglow.survey.application.dto.SubmitSurveyAnswerCommand
import com.sleekydz86.idolglow.survey.application.dto.SubmitSurveyResponseCommand
import com.sleekydz86.idolglow.survey.application.dto.UpsertSurveyFormCommand
import com.sleekydz86.idolglow.survey.application.dto.UpsertSurveyQuestionCommand

fun AdminUpsertSurveyFormRequest.toCommand(): UpsertSurveyFormCommand =
    UpsertSurveyFormCommand(
        title = title,
        description = description,
        descriptionTags = descriptionTags,
        status = status,
        primaryCategory = primaryCategory,
        secondaryCategory = secondaryCategory,
        questions =
            questions.map { question ->
                UpsertSurveyQuestionCommand(
                    order = question.order,
                    title = question.title,
                    description = question.description,
                    type = question.type,
                    required = question.required,
                    options = question.options,
                )
            },
    )

fun SubmitSurveyResponseRequest.toCommand(): SubmitSurveyResponseCommand =
    SubmitSurveyResponseCommand(
        answers =
            answers.map { answer ->
                SubmitSurveyAnswerCommand(
                    questionId = answer.questionId,
                    answerText = answer.answerText,
                    selectedOptions = answer.selectedOptions,
                )
            },
    )
