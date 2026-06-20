package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle

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
