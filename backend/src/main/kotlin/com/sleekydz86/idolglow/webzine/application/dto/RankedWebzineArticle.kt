package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.IssueCategory
import java.time.LocalDate

data class RankedWebzineArticle(
    val articleId: Long,
    val articleSlug: String,
    val title: String,
    val summary: String,
    val cardImageUrl: String,
    val category: IssueCategory,
    val issueSlug: String,
    val volume: Int,
    val issueDate: LocalDate,
    val tags: List<String>,
    val likeCount: Long,
    val viewerCount: Long,
) {
    val contentKey: String
        get() = "$issueSlug/$articleSlug"
}
