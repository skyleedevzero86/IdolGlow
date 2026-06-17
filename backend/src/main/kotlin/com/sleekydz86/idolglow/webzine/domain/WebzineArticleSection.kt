package com.sleekydz86.idolglow.webzine.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint

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
