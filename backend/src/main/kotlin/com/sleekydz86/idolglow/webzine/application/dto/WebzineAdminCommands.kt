package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.IssueCategory

data class CreateWebzineIssueCommand(
    val volume: Int,
    val issueDate: String,
    val coverImageUrl: String,
    val teaser: String,
)

data class UpsertWebzineArticleCommand(
    val title: String,
    val kicker: String,
    val summary: String,
    val category: IssueCategory,
    val formatLabel: String,
    val heroImageUrl: String,
    val cardImageUrl: String,
    val galleryImageUrls: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val authorName: String,
    val authorEmail: String,
    val creditLine: String,
    val highlightQuote: String? = null,
    val sections: List<UpsertWebzineArticleSectionCommand>,
)

data class UpsertWebzineArticleSectionCommand(
    val heading: String? = null,
    val body: String,
    val note: String? = null,
)
