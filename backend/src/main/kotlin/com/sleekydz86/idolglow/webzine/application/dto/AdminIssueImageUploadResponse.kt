package com.sleekydz86.idolglow.webzine.application.dto

data class AdminIssueImageUploadResponse(
    val url: String,
    val objectKey: String,
    val contentType: String,
    val size: Long,
)
