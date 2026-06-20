package com.sleekydz86.idolglow.newsletter.application.dto

data class AdminNewsletterImageUploadResponse(
    val url: String,
    val objectKey: String,
    val contentType: String,
    val size: Long,
)
