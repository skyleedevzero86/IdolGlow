package com.sleekydz86.idolglow.newsletter.domain

import com.sleekydz86.idolglow.global.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
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
import java.time.LocalDate

data class NewsletterDraft(
    val title: String,
    val categoryLabel: String,
    val publishedAt: LocalDate,
    val imageUrl: String,
    val summary: String,
    val tags: List<String>,
    val paragraphs: List<String>,
)

@Entity
@Table(
    name = "newsletters",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_newsletter_slug", columnNames = ["slug"]),
    ]
)
class Newsletter(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, length = 160)
    var slug: String,

    @Column(nullable = false, length = 200)
    var title: String,

    @Column(name = "category_label", nullable = false, length = 80)
    var categoryLabel: String,

    @Column(name = "published_at", nullable = false)
    var publishedAt: LocalDate,

    @Column(name = "image_url", nullable = false, length = 500)
    var imageUrl: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var summary: String,

    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "newsletter", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val tags: MutableList<NewsletterTag> = mutableListOf(),

    @OrderBy("displayOrder ASC")
    @OneToMany(mappedBy = "newsletter", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val paragraphs: MutableList<NewsletterParagraph> = mutableListOf(),
) : BaseEntity() {

    fun update(slug: String, draft: NewsletterDraft) {
        validateDraft(draft)

        this.slug = slug.trim()
        this.title = draft.title.trim()
        this.categoryLabel = draft.categoryLabel.trim()
        this.publishedAt = draft.publishedAt
        this.imageUrl = draft.imageUrl.trim()
        this.summary = draft.summary.trim()

        replaceTags(draft.tags)
        replaceParagraphs(draft.paragraphs)
    }

    private fun replaceTags(tagNames: List<String>) {
        tags.clear()
        tagNames.forEachIndexed { index, tag ->
            tags += NewsletterTag(
                newsletter = this,
                displayOrder = index,
                tagName = tag.trim(),
            )
        }
    }

    private fun replaceParagraphs(items: List<String>) {
        paragraphs.clear()
        items.forEachIndexed { index, body ->
            paragraphs += NewsletterParagraph(
                newsletter = this,
                displayOrder = index,
                body = body.trim(),
            )
        }
    }

    companion object {
        fun create(slug: String, draft: NewsletterDraft): Newsletter {
            validateDraft(draft)

            return Newsletter(
                slug = slug.trim(),
                title = draft.title.trim(),
                categoryLabel = draft.categoryLabel.trim(),
                publishedAt = draft.publishedAt,
                imageUrl = draft.imageUrl.trim(),
                summary = draft.summary.trim(),
            ).apply {
                replaceTags(draft.tags)
                replaceParagraphs(draft.paragraphs)
            }
        }

        private fun validateDraft(draft: NewsletterDraft) {
            require(draft.title.isNotBlank()) { "소식지 제목은 비어 있을 수 없습니다." }
            require(draft.categoryLabel.isNotBlank()) { "카테고리명은 비어 있을 수 없습니다." }
            require(draft.imageUrl.isNotBlank()) { "대표 이미지는 비어 있을 수 없습니다." }
            require(draft.summary.isNotBlank()) { "요약은 비어 있을 수 없습니다." }
            require(draft.paragraphs.isNotEmpty()) { "본문 문단은 최소 1개 이상이어야 합니다." }
            require(draft.paragraphs.any { it.isNotBlank() }) { "본문 문단은 비어 있을 수 없습니다." }
        }
    }
}

@Entity
@Table(
    name = "newsletter_tags",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_newsletter_tag_name", columnNames = ["newsletter_id", "tag_name"]),
    ]
)
class NewsletterTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "newsletter_id", nullable = false)
    val newsletter: Newsletter,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(name = "tag_name", nullable = false, length = 80)
    val tagName: String,
) : BaseEntity()

@Entity
@Table(
    name = "newsletter_paragraphs",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_newsletter_paragraph_order", columnNames = ["newsletter_id", "display_order"]),
    ]
)
class NewsletterParagraph(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "newsletter_id", nullable = false)
    val newsletter: Newsletter,

    @Column(name = "display_order", nullable = false)
    val displayOrder: Int,

    @Column(nullable = false, columnDefinition = "TEXT")
    val body: String,
) : BaseEntity()
