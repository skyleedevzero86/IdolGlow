package com.sleekydz86.idolglow.subscription.application.event

import java.time.LocalDate
import java.time.LocalDateTime

data class WebzineIssueDispatchRequestedEvent(
    val slug: String,
    val volume: Int,
    val issueDate: LocalDate,
    val teaser: String,
    val coverImageUrl: String,
    val articleTitles: List<String>,
    val contentCreatedAt: LocalDateTime?,
)
