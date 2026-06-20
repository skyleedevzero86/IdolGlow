package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle

data class AdminIssueArticleCardResponse(
    val id: Long,
    val slug: String,
    val issueSlug: String,
    val volume: Int,
    val issueDate: String,
    val title: String,
    val kicker: String,
    val summary: String,
    val cardImageUrl: String,
    val category: String,
    val categoryLabel: String,
    val formatLabel: String,
    val tags: List<String>,
    val authorName: String,
) {
    companion object {
        fun from(article: WebzineArticle): AdminIssueArticleCardResponse =
            AdminIssueArticleCardResponse(
                id = article.id,
                slug = article.slug,
                issueSlug = article.issue.slug,
                volume = article.issue.volume,
                issueDate = article.issue.issueDate.asIssueDisplayValue(),
                title = article.title,
                kicker = article.kicker,
                summary = article.summary,
                cardImageUrl = article.cardImageUrl,
                category = article.category.code,
                categoryLabel = article.category.label,
                formatLabel = article.formatLabel,
                tags = article.tags.map { it.tagName },
                authorName = article.authorName,
            )
    }
}
