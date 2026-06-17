package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSection
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class AdminIssueSummaryResponse(
    val id: Long,
    val slug: String,
    val volume: Int,
    val issueDate: String,
    val issueYear: Int,
    val issueMonth: Int,
    val coverImageUrl: String,
    val teaser: String,
    val articleCount: Int,
    val headlines: List<AdminIssueHeadlineResponse>,
) {
    companion object {
        fun from(issue: WebzineIssue): AdminIssueSummaryResponse =
            AdminIssueSummaryResponse(
                id = issue.id,
                slug = issue.slug,
                volume = issue.volume,
                issueDate = issue.issueDate.asIssueDisplayValue(),
                issueYear = issue.issueDate.year,
                issueMonth = issue.issueDate.monthValue,
                coverImageUrl = issue.coverImageUrl,
                teaser = issue.teaser,
                articleCount = issue.articles.size,
                headlines = issue.articles
                    .sortedByDescending { it.createdAt ?: LocalDateTime.MIN }
                    .take(6)
                    .map(AdminIssueHeadlineResponse::from)
            )
    }
}
