package com.sleekydz86.idolglow.idolglowissue.adapter.web.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpsertIdolGlowArticleSectionRequest(
    @field:Size(max = 200)
    val heading: String? = null,
    @field:NotBlank
    val body: String,
    @field:Size(max = 1000)
    val note: String? = null,
)
