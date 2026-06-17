package com.sleekydz86.idolglow.event.ui.dto

data class AdminEventPageResponse(
    val items: List<AdminEventSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
)
