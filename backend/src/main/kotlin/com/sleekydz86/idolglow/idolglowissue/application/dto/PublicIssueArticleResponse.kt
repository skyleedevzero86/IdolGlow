package com.sleekydz86.idolglow.idolglowissue.application.dto

import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticle

data class PublicIssueArticleResponse(
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
    val creditLine: String,
    val highlightQuote: String?,
    val sections: List<PublicIssueArticleSectionResponse>,
    val previousArticle: PublicIssueHeadlineResponse?,
    val nextArticle: PublicIssueHeadlineResponse?,
    val relatedArticles: List<PublicIssueArticleCardResponse>,
) {
    companion object {
        private const val RELATED_LIMIT = 6

        fun from(
            article: IdolGlowArticle,
            issueArticles: List<IdolGlowArticle>,
        ): PublicIssueArticleResponse {
            val index = issueArticles.indexOfFirst { it.id == article.id }
            return PublicIssueArticleResponse(
                slug = article.slug,
                issueSlug = article.issue.slug,
                volume = article.issue.volume,
                issueDate = article.issue.issueDate.asIssueDisplayValue(),
                title = article.title,
                kicker = article.kicker,
                summary = article.summary,
                heroImageUrl = article.heroImageUrl,
                cardImageUrl = article.cardImageUrl,
                galleryImageUrls = article.galleryImages.sortedBy { it.displayOrder }.map { it.imageUrl }.distinct(),
                category = article.category.code,
                categoryLabel = article.category.label,
                formatLabel = article.formatLabel,
                tags = article.tags.sortedBy { it.displayOrder }.map { it.tagName },
                authorName = article.authorName,
                creditLine = article.creditLine,
                highlightQuote = article.highlightQuote,
                sections = article.sections.sortedBy { it.displayOrder }.map(PublicIssueArticleSectionResponse::from),
                previousArticle = issueArticles.headlineAt(index - 1),
                nextArticle = issueArticles.headlineAt(index + 1),
                relatedArticles =
                    issueArticles
                        .mapIndexed { order, related -> order to related }
                        .filter { (_, related) -> related.id != article.id }
                        .take(RELATED_LIMIT)
                        .map { (order, related) -> PublicIssueArticleCardResponse.from(related, order) },
            )
        }

        private fun List<IdolGlowArticle>.headlineAt(index: Int): PublicIssueHeadlineResponse? =
            getOrNull(index)?.let { PublicIssueHeadlineResponse.from(it, index) }
    }
}
