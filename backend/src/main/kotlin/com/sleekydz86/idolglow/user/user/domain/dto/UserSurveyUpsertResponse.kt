package com.sleekydz86.idolglow.user.user.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

data class UserSurveyUpsertResponse(
    @field:Schema(description = "생성된 설문조사 ID", example = "1")
    val id: Long
)