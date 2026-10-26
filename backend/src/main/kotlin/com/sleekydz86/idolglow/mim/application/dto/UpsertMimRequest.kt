package com.sleekydz86.idolglow.mim.application.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class UpsertMimRequest(
    @field:NotBlank
    val imageName: String,
    val imagePath: String?,
    val imageFileName: String?,
    @field:Size(max = 500)
    val linkUrl: String?,
    @field:Size(max = 20)
    val categoryLabel: String? = null,
    val description: String?,
    val activeYn: String?,
    val createdBy: String?,
)
