package com.sleekydz86.idolglow.webzine.adapter.web.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpsertWebzineArticleSectionRequest(
    @field:Size(max = 200)
    val heading: String? = null,
    @field:NotBlank
    val body: String,
    @field:Size(max = 1000)
    val note: String? = null,
)
