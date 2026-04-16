package com.sleekydz86.idolglow.bnr.application.dto

import jakarta.validation.constraints.NotBlank

data class UpsertBnrRequest(
    @field:NotBlank
    val bannerName: String,
    val linkUrl: String?,
    val imagePath: String?,
    val imageFileName: String?,
    val description: String?,
    val sortOrder: Int?,
    val activeYn: String?,
    val createdBy: String?,
)
