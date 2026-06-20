package com.sleekydz86.idolglow.newsletter.domain

import java.time.LocalDate

data class NewsletterDraft(
    val title: String,
    val categoryLabel: String,
    val publishedAt: LocalDate,
    val imageUrl: String,
    val summary: String,
    val tags: List<String>,
    val paragraphs: List<String>,
)
