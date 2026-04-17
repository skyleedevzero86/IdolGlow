package com.sleekydz86.idolglow.mim.application.dto

import jakarta.validation.constraints.NotBlank

data class UpsertMimRequest(
    @field:NotBlank
    val imageName: String,
    val imagePath: String?,
    val imageFileName: String?,
    val description: String?,
    val activeYn: String?,
    val createdBy: String?,
)
