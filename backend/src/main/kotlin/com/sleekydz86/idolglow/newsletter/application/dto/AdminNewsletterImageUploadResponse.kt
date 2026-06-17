package com.sleekydz86.idolglow.newsletter.application.dto

import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AdminNewsletterImageUploadResponse(
    val url: String,
    val objectKey: String,
    val contentType: String,
    val size: Long,
)
