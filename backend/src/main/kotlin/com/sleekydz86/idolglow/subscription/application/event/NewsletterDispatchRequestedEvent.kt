package com.sleekydz86.idolglow.subscription.application.event

import java.time.LocalDate
import java.time.LocalDateTime

data class NewsletterDispatchRequestedEvent(
    val slug: String,
    val title: String,
    val summary: String,
    val imageUrl: String,
    val publishedAt: LocalDate,
    val tags: List<String>,
    val paragraphs: List<String>,
    val contentCreatedAt: LocalDateTime?,
)
