package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle

data class AdminIssueHeadlineResponse(
    val slug: String,
    val title: String,
    val category: String,
    val categoryLabel: String,
) {
    companion object {
        fun from(article: WebzineArticle): AdminIssueHeadlineResponse =
            AdminIssueHeadlineResponse(
                slug = article.slug,
                title = article.title,
                category = article.category.code,
                categoryLabel = article.category.label,
            )
    }
}
