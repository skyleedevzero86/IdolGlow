package com.sleekydz86.idolglow.newsletter.application.dto

data class AdminNewsletterPageResponse(
    val newsletters: List<AdminNewsletterSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)
