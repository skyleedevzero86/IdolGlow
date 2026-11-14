package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle

data class PublicIssueHeadlineResponse(
    val slug: String,
    val order: Int,
    val title: String,
    val category: String,
    val categoryLabel: String,
) {
    companion object {
        fun from(
            article: WebzineArticle,
            order: Int,
        ): PublicIssueHeadlineResponse =
            PublicIssueHeadlineResponse(
                slug = article.slug,
                order = order,
                title = article.title,
                category = article.category.code,
                categoryLabel = article.category.label,
            )
    }
}
