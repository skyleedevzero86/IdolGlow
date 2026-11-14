package com.sleekydz86.idolglow.idolglowissue.application.dto

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticle

data class AdminIssueRelatedContentResponse(
    val id: Long,
    val slug: String,
    val title: String,
    val category: String,
    val categoryLabel: String,
    val imageUrl: String,
) {
    companion object {
        fun from(article: IdolGlowArticle): AdminIssueRelatedContentResponse =
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
