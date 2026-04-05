package com.sleekydz86.idolglow.webzine.application.dto

import com.sleekydz86.idolglow.webzine.domain.WebzineArticle
import com.sleekydz86.idolglow.webzine.domain.WebzineArticleSection
import com.sleekydz86.idolglow.webzine.domain.WebzineIssue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

data class AdminIssuePageResponse(
    val issues: List<AdminIssueSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
    val latestVolume: Int,
    val totalArticleCount: Int,
    val availableYears: List<Int>,
    val availableMonths: List<Int>,
    val availableVolumes: List<Int>,
)

data class AdminIssueImageUploadResponse(
    val url: String,
    val objectKey: String,
    val contentType: String,
    val size: Long,
)

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

data class AdminIssueVolumeResponse(
    val id: Long,
    val slug: String,
    val volume: Int,
    val issueDate: String,
    val issueYear: Int,
    val issueMonth: Int,
    val coverImageUrl: String,
    val teaser: String,
    val articleCount: Int,
    val articles: List<AdminIssueArticleCardResponse>,
) {
    companion object {
        fun from(issue: WebzineIssue): AdminIssueVolumeResponse =
            AdminIssueVolumeResponse(
                id = issue.id,
                slug = issue.slug,
                volume = issue.volume,
                issueDate = issue.issueDate.asIssueDisplayValue(),
                issueYear = issue.issueDate.year,
                issueMonth = issue.issueDate.monthValue,
                coverImageUrl = issue.coverImageUrl,
                teaser = issue.teaser,
                articleCount = issue.articles.size,
                articles = issue.articles
                    .sortedByDescending { it.createdAt ?: LocalDateTime.MIN }
                    .map(AdminIssueArticleCardResponse::from)
            )
    }
}

data class AdminIssueArticleCardResponse(
    val id: Long,
    val slug: String,
    val issueSlug: String,
    val volume: Int,
    val issueDate: String,
    val title: String,
    val kicker: String,
    val summary: String,
    val cardImageUrl: String,
    val category: String,
    val categoryLabel: String,
    val formatLabel: String,
    val tags: List<String>,
    val authorName: String,
) {
    companion object {
        fun from(article: WebzineArticle): AdminIssueArticleCardResponse =
            AdminIssueArticleCardResponse(
                id = article.id,
                slug = article.slug,
                issueSlug = article.issue.slug,
                volume = article.issue.volume,
                issueDate = article.issue.issueDate.asIssueDisplayValue(),
                title = article.title,
                kicker = article.kicker,
                summary = article.summary,
                cardImageUrl = article.cardImageUrl,
                category = article.category.code,
                categoryLabel = article.category.label,
                formatLabel = article.formatLabel,
                tags = article.tags.map { it.tagName },
                authorName = article.authorName,
            )
    }
}

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

data class AdminIssueArticleSectionResponse(
    val id: Long,
    val heading: String?,
    val body: String,
    val paragraphs: List<String>,
    val note: String?,
) {
    companion object {
        fun from(section: WebzineArticleSection): AdminIssueArticleSectionResponse =
            AdminIssueArticleSectionResponse(
                id = section.id,
                heading = section.heading,
                body = section.body,
                paragraphs = section.body
                    .split(Regex("\\n{2,}"))
                    .map { it.trim() }
                    .filter { it.isNotBlank() },
                note = section.note,
            )
    }
}

data class AdminIssueRelatedContentResponse(
    val id: Long,
    val slug: String,
    val title: String,
    val category: String,
    val categoryLabel: String,
    val imageUrl: String,
) {
    companion object {
        fun from(article: WebzineArticle): AdminIssueRelatedContentResponse =
            AdminIssueRelatedContentResponse(
                id = article.id,
                slug = article.slug,
                title = article.title,
                category = article.category.code,
                categoryLabel = article.category.label,
                imageUrl = article.cardImageUrl.ifBlank { article.heroImageUrl },
            )
    }
}

private val issueDateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.")

private fun LocalDate.asIssueDisplayValue(): String = format(issueDateFormatter)
