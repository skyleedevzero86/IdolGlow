package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineIssue

data class PublicIssueSummaryResponse(
    val slug: String,
    val volume: Int,
    val issueDate: String,
    val issueYear: Int,
    val issueMonth: Int,
    val coverImageUrl: String,
    val teaser: String,
    val articleCount: Int,
    val leadArticleSlug: String?,
    val articles: List<PublicIssueHeadlineResponse>,
) {
    companion object {
        fun from(issue: WebzineIssue): PublicIssueSummaryResponse {
            val articles = issue.articlesByLatest()
            return PublicIssueSummaryResponse(
                slug = issue.slug,
                volume = issue.volume,
                issueDate = issue.issueDate.asIssueDisplayValue(),
                issueYear = issue.issueDate.year,
                issueMonth = issue.issueDate.monthValue,
                coverImageUrl = issue.coverImageUrl,
                teaser = issue.teaser,
                articleCount = articles.size,
                leadArticleSlug = articles.firstOrNull()?.slug,
                articles = articles.mapIndexed { index, article -> PublicIssueHeadlineResponse.from(article, index) },
            )
        }
    }
}
