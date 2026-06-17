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
