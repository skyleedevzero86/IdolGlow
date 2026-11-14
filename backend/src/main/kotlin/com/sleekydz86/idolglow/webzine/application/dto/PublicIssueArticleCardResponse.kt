package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle

data class PublicIssueArticleCardResponse(
    val slug: String,
    val order: Int,
    val title: String,
    val kicker: String,
    val summary: String,
    val cardImageUrl: String,
    val category: String,
    val categoryLabel: String,
    val formatLabel: String,
) {
    companion object {
        fun from(
            article: WebzineArticle,
            order: Int,
        ): PublicIssueArticleCardResponse =
            PublicIssueArticleCardResponse(
                slug = article.slug,
                order = order,
                title = article.title,
                kicker = article.kicker,
                summary = article.summary,
                cardImageUrl = article.cardImageUrl.ifBlank { article.heroImageUrl },
                category = article.category.code,
                categoryLabel = article.category.label,
                formatLabel = article.formatLabel,
            )
    }
}
