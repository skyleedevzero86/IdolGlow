package com.sleekydz86.idolglow.survey.domain.dto

import com.sleekydz86.idolglow.survey.domain.SurveyForm

internal fun SurveyForm.descriptionMarkdown(): String? =
    descriptionContent?.markdown?.takeIf { it.isNotBlank() }
        ?: description.takeIf { descriptionContent == null }

internal fun SurveyForm.descriptionTagNames(): List<String> =
    descriptionContent
        ?.tags
        ?.sortedBy { it.displayOrder }
        ?.map { it.tagName }
        ?: emptyList()
