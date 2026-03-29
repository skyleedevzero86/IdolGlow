package com.sleekydz86.idolglow.user.user.application.dto

import com.sleekydz86.idolglow.user.user.domain.vo.ConceptType
import java.time.LocalDate

data class UpsertUserSurveyCommand (
    val concept: ConceptType,
    val idolName: String,
    val visitStartDate: LocalDate,
    val visitEndDate: LocalDate,
    val places: List<String>
)
