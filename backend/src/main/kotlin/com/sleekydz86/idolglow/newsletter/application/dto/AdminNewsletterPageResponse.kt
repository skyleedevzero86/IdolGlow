package com.sleekydz86.idolglow.newsletter.application.dto

import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AdminNewsletterPageResponse(
    val newsletters: List<AdminNewsletterSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)
