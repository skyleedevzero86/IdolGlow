package com.sleekydz86.idolglow.pup.application.dto

import jakarta.validation.constraints.NotBlank

data class UpsertPupRequest(
    @field:NotBlank
    val title: String,
    val fileUrl: String?,
    val linkTarget: String?,
    val imagePath: String?,
    val imageFileName: String?,
    val noticeStartDate: String?,
    val noticeEndDate: String?,
    val stopViewYn: String?,
    val noticeYn: String?,
    val createdBy: String?,
    val updatedBy: String?,
)
