package com.sleekydz86.idolglow.newsletter.application.dto

import com.sleekydz86.idolglow.newsletter.domain.Newsletter
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class AdminNewsletterDetailResponse(
    val id: Long,
    val slug: String,
    val title: String,
    val categoryLabel: String,
    val publishedAt: String,
    val imageUrl: String,
    val tags: List<String>,
    val summary: String,
    val paragraphs: List<String>,
) {
    companion object {
        fun from(newsletter: Newsletter): AdminNewsletterDetailResponse =
            AdminNewsletterDetailResponse(
                id = newsletter.id,
                slug = newsletter.slug,
                title = newsletter.title,
                categoryLabel = newsletter.categoryLabel,
                publishedAt = newsletter.publishedAt.asNewsletterDisplayValue(),
                imageUrl = newsletter.imageUrl,
                tags = newsletter.tags.map { it.tagName },
                summary = newsletter.summary,
                paragraphs = newsletter.paragraphs.map { it.body },
            )
    }
}
