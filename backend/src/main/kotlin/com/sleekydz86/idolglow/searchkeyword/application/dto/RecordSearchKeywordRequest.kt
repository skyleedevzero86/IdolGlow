package com.sleekydz86.idolglow.searchkeyword.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RecordSearchKeywordRequest(
    @field:NotBlank
    @field:Size(max = 100)
    val keyword: String,
)
