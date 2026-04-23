package com.sleekydz86.idolglow.webzine.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.OneToMany
import jakarta.persistence.OrderBy
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.LocalDate

@Entity
@Table(
    name = "webzine_issues",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_webzine_issue_slug", columnNames = ["slug"]),
        UniqueConstraint(name = "uk_webzine_issue_volume", columnNames = ["volume"]),
    ]
)
class WebzineIssue(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @Column(nullable = false, length = 80)
    var slug: String,

    @Column(nullable = false)
    var volume: Int,

    @Column(name = "issue_date", nullable = false)
    var issueDate: LocalDate,

    @Column(name = "cover_image_url", nullable = false, length = 500)
    var coverImageUrl: String,

    @Column(nullable = false, length = 1000)
    var teaser: String,

    @OrderBy("createdAt DESC")
    @OneToMany(mappedBy = "issue", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val articles: MutableList<WebzineArticle> = mutableListOf(),
) : BaseEntity() {

    fun update(issueDate: LocalDate, coverImageUrl: String, teaser: String) {
        this.issueDate = issueDate
        this.coverImageUrl = coverImageUrl
        this.teaser = teaser
    }

    fun addArticle(article: WebzineArticle) {
        require(article.issue === this) { "같은 호에 속한 기사만 추가할 수 있습니다." }
        articles.add(0, article)
    }

    companion object {
        fun create(
            slug: String,
            volume: Int,
            issueDate: LocalDate,
            coverImageUrl: String,
            teaser: String,
        ): WebzineIssue {
            require(volume > 0) { "발행 호는 1 이상이어야 합니다." }
            require(slug.isNotBlank()) { "호 슬러그는 비어 있을 수 없습니다." }
            require(coverImageUrl.isNotBlank()) { "표지 이미지는 비어 있을 수 없습니다." }
            require(teaser.isNotBlank()) { "소개 문구는 비어 있을 수 없습니다." }

            return WebzineIssue(
                slug = slug.trim(),
                volume = volume,
                issueDate = issueDate,
                coverImageUrl = coverImageUrl.trim(),
                teaser = teaser.trim(),
            )
        }
    }
}
