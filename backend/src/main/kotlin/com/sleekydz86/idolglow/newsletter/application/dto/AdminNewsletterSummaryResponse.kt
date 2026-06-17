package com.sleekydz86.idolglow.newsletter.application.dto

import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AdminNewsletterSummaryResponse(
    val id: Long,
    val slug: String,
    val title: String,
    val categoryLabel: String,
    val publishedAt: String,
    val imageUrl: String,
    val tags: List<String>,
    val summary: String,
) {
    companion object {
        fun from(newsletter: Newsletter): AdminNewsletterSummaryResponse =
            AdminNewsletterSummaryResponse(
                id = newsletter.id,
                slug = newsletter.slug,
                title = newsletter.title,
                categoryLabel = newsletter.categoryLabel,
                publishedAt = newsletter.publishedAt.asNewsletterDisplayValue(),
                imageUrl = newsletter.imageUrl,
                tags = newsletter.tags.map { it.tagName },
                summary = newsletter.summary,
            )
    }
}
