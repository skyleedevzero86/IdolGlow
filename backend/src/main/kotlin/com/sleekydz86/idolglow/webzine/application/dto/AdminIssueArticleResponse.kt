package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle

data class AdminIssueArticleResponse(
    val id: Long,
    val slug: String,
    val issueSlug: String,
    val volume: Int,
    val issueDate: String,
    val title: String,
    val kicker: String,
    val summary: String,
    val heroImageUrl: String,
    val cardImageUrl: String,
    val galleryImageUrls: List<String>,
    val category: String,
    val categoryLabel: String,
    val formatLabel: String,
    val tags: List<String>,
    val authorName: String,
    val authorEmail: String,
    val creditLine: String,
    val highlightQuote: String?,
    val sections: List<AdminIssueArticleSectionResponse>,
    val relatedContents: List<AdminIssueRelatedContentResponse>,
) {
    companion object {
        fun from(
            article: WebzineArticle,
            relatedContents: List<AdminIssueRelatedContentResponse>,
        ): AdminIssueArticleResponse =
            AdminIssueArticleResponse(
                id = article.id,
                slug = article.slug,
                issueSlug = article.issue.slug,
                volume = article.issue.volume,
                issueDate = article.issue.issueDate.asIssueDisplayValue(),
                title = article.title,
                kicker = article.kicker,
                summary = article.summary,
                heroImageUrl = article.heroImageUrl,
                cardImageUrl = article.cardImageUrl,
                galleryImageUrls = buildGallery(article),
                category = article.category.code,
                categoryLabel = article.category.label,
                formatLabel = article.formatLabel,
                tags = article.tags.map { it.tagName },
                authorName = article.authorName,
                authorEmail = article.authorEmail,
                creditLine = article.creditLine,
                highlightQuote = article.highlightQuote,
                sections = article.sections.map(AdminIssueArticleSectionResponse::from),
                relatedContents = relatedContents,
            )

        private fun buildGallery(article: WebzineArticle): List<String> =
            buildList {
                add(article.heroImageUrl)
                add(article.cardImageUrl)
                addAll(article.galleryImages.map { it.imageUrl })
            }.distinct()
    }
}
