package com.sleekydz86.idolglow.user.user.application.dto

import java.time.LocalDate

data class UpsertUserSurveyCommand (
    val concept: ConceptType,
    val idolName: String,
    val visitStartDate: LocalDate,
    val visitEndDate: LocalDate,
    val places: List<String>
)