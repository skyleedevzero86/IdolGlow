package com.sleekydz86.idolglow.newsletter.domain

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
    name = "newsletter_tags",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_newsletter_tag_name", columnNames = ["newsletter_id", "tag_name"]),
    ],
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
