package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineIssue

data class PublicIssueDetailResponse(
    val slug: String,
    val volume: Int,
    val issueDate: String,
    val issueYear: Int,
    val issueMonth: Int,
    val coverImageUrl: String,
    val teaser: String,
    val articleCount: Int,
    val articles: List<PublicIssueArticleCardResponse>,
) {
    companion object {
        fun from(issue: WebzineIssue): PublicIssueDetailResponse {
            val articles = issue.articlesByLatest()
            return PublicIssueDetailResponse(
                slug = issue.slug,
                volume = issue.volume,
                issueDate = issue.issueDate.asIssueDisplayValue(),
                issueYear = issue.issueDate.year,
                issueMonth = issue.issueDate.monthValue,
                coverImageUrl = issue.coverImageUrl,
                teaser = issue.teaser,
                articleCount = articles.size,
                articles = articles.mapIndexed { index, article -> PublicIssueArticleCardResponse.from(article, index) },
            )
        }
    }
}
