package com.sleekydz86.idolglow.webzine.domain

import com.sleekydz86.idolglow.global.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

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

data class WebzineArticleSectionDraft(
    val heading: String?,
    val body: String,
    val note: String?,
)

@Entity
@Table(
    name = "webzine_articles",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_webzine_article_issue_slug", columnNames = ["issue_id", "slug"])
    ]
)
class WebzineArticle(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "issue_id", nullable = false)
    val issue: WebzineIssue,

    @Column(nullable = false, length = 150)
    var slug: String,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(nullable = false, length = 200)
    var kicker: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var summary: String,

    @Column(name = "hero_image_url", nullable = false, length = 500)
    var heroImageUrl: String,

    @Column(name = "card_image_url", nullable = false, length = 500)
    var cardImageUrl: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var category: IssueCategory,

    @Column(name = "format_label", nullable = false, length = 60)
    var formatLabel: String,

    @Column(name = "author_name", nullable = false, length = 120)
    var authorName: String,

    @Column(name = "author_email", nullable = false, length = 255)
    var authorEmail: String,

    @Column(name = "credit_line", nullable = false, length = 255)
    var creditLine: String,

    @Column(name = "highlight_quote", length = 500)
    var highlightQuote: String? = null,

    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "article", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val sections: MutableList<WebzineArticleSection> = mutableListOf(),

    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "article", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val galleryImages: MutableList<WebzineArticleGalleryImage> = mutableListOf(),

    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "article", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val tags: MutableList<WebzineArticleTag> = mutableListOf(),
) : BaseEntity() {

    fun update(slug: String, draft: WebzineArticleDraft) {
        this.slug = slug.trim()
        this.title = draft.title.trim()
        this.kicker = draft.kicker.trim()
        this.summary = draft.summary.trim()
        this.heroImageUrl = draft.heroImageUrl.trim()
        this.cardImageUrl = draft.cardImageUrl.trim()
        this.category = draft.category
        this.formatLabel = draft.formatLabel.trim()
        this.authorName = draft.authorName.trim()
        this.authorEmail = draft.authorEmail.trim()
        this.creditLine = draft.creditLine.trim()
        this.highlightQuote = draft.highlightQuote?.trim()?.takeIf { it.isNotEmpty() }

        replaceSections(draft.sections)
        replaceGalleryImages(draft.galleryImageUrls)
        replaceTags(draft.tags)
    }

    private fun replaceSections(sectionDrafts: List<WebzineArticleSectionDraft>) {
        sections.clear()
        sectionDrafts.forEachIndexed { index, section ->
            sections += WebzineArticleSection(
                article = this,
                displayOrder = index,
                heading = section.heading?.trim()?.takeIf { it.isNotEmpty() },
                body = section.body.trim(),
                note = section.note?.trim()?.takeIf { it.isNotEmpty() },
            )
        }
    }

    private fun replaceGalleryImages(imageUrls: List<String>) {
        galleryImages.clear()
        imageUrls.forEachIndexed { index, imageUrl ->
            galleryImages += WebzineArticleGalleryImage(
                article = this,
                displayOrder = index,
                imageUrl = imageUrl.trim(),
            )
        }
    }

    private fun replaceTags(tagNames: List<String>) {
        tags.clear()
        tagNames.forEachIndexed { index, tag ->
            tags += WebzineArticleTag(
                article = this,
                displayOrder = index,
                tagName = tag.trim(),
            )
        }
    }

    companion object {
        fun create(
            issue: WebzineIssue,
            slug: String,
            draft: WebzineArticleDraft,
        ): WebzineArticle {
            validateDraft(draft)

            return WebzineArticle(
                issue = issue,
                slug = slug.trim(),
                title = draft.title.trim(),
                kicker = draft.kicker.trim(),
                summary = draft.summary.trim(),
                heroImageUrl = draft.heroImageUrl.trim(),
                cardImageUrl = draft.cardImageUrl.trim(),
                category = draft.category,
                formatLabel = draft.formatLabel.trim(),
                authorName = draft.authorName.trim(),
                authorEmail = draft.authorEmail.trim(),
                creditLine = draft.creditLine.trim(),
                highlightQuote = draft.highlightQuote?.trim()?.takeIf { it.isNotEmpty() },
            ).apply {
                replaceSections(draft.sections)
                replaceGalleryImages(draft.galleryImageUrls)
                replaceTags(draft.tags)
            }
        }

        private fun validateDraft(draft: WebzineArticleDraft) {
            require(draft.title.isNotBlank()) { "기사 제목은 비어 있을 수 없습니다." }
            require(draft.kicker.isNotBlank()) { "부제는 비어 있을 수 없습니다." }
            require(draft.summary.isNotBlank()) { "요약은 비어 있을 수 없습니다." }
            require(draft.heroImageUrl.isNotBlank()) { "대표 이미지는 비어 있을 수 없습니다." }
            require(draft.cardImageUrl.isNotBlank()) { "카드 이미지는 비어 있을 수 없습니다." }
            require(draft.formatLabel.isNotBlank()) { "형식 라벨은 비어 있을 수 없습니다." }
            require(draft.authorName.isNotBlank()) { "작성자명은 비어 있을 수 없습니다." }
            require(draft.authorEmail.isNotBlank()) { "작성자 이메일은 비어 있을 수 없습니다." }
            require(draft.creditLine.isNotBlank()) { "크레딧은 비어 있을 수 없습니다." }
            require(draft.sections.isNotEmpty()) { "본문 섹션은 최소 1개 이상이어야 합니다." }
        }
    }
}

@Entity
@Table(
    name = "webzine_article_sections",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_webzine_article_section_order", columnNames = ["article_id", "display_order"])
    ]
)
class WebzineArticleSection(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    val article: WebzineArticle,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(length = 200)
    val heading: String? = null,

    @Column(nullable = false, columnDefinition = "TEXT")
    val body: String,

    @Column(length = 1000)
    val note: String? = null,
) : BaseEntity()

@Entity
@Table(
    name = "webzine_article_gallery_images",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_webzine_article_gallery_order", columnNames = ["article_id", "display_order"])
    ]
)
class WebzineArticleGalleryImage(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    val article: WebzineArticle,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(name = "image_url", nullable = false, length = 500)
    val imageUrl: String,
) : BaseEntity()

@Entity
@Table(
    name = "webzine_article_tags",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_webzine_article_tag_name", columnNames = ["article_id", "tag_name"])
    ]
)
class WebzineArticleTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "article_id", nullable = false)
    val article: WebzineArticle,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(name = "tag_name", nullable = false, length = 80)
    val tagName: String,
) : BaseEntity()
