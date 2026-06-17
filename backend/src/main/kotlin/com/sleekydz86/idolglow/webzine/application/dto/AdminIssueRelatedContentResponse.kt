package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSection
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class AdminIssueRelatedContentResponse(
    val id: Long,
    val slug: String,
    val title: String,
    val category: String,
    val categoryLabel: String,
    val imageUrl: String,
) {
    companion object {
        fun from(article: WebzineArticle): AdminIssueRelatedContentResponse =
            AdminIssueRelatedContentResponse(
                id = article.id,
                slug = article.slug,
                title = article.title,
                category = article.category.code,
                categoryLabel = article.category.label,
                imageUrl = article.cardImageUrl.ifBlank { article.heroImageUrl },
            )
    }
}
