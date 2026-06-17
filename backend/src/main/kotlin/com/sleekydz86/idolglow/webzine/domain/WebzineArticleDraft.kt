package com.sleekydz86.idolglow.webzine.domain

data class WebzineArticleDraft(
    val title: String,
    val kicker: String,
    val summary: String,
    val heroImageUrl: String,
    val cardImageUrl: String,
    val category: IssueCategory,
    val formatLabel: String,
    val authorName: String,
    val authorEmail: String,
    val creditLine: String,
    val highlightQuote: String?,
    val sections: List<WebzineArticleSectionDraft>,
    val galleryImageUrls: List<String>,
    val tags: List<String>,
)
