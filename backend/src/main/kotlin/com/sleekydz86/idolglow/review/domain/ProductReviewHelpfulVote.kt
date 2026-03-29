package com.sleekydz86.idolglow.review.domain

import com.sleekydz86.idolglow.global.BaseEntity
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
    name = "product_review_helpful_votes",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_review_helpful_user", columnNames = ["review_id", "user_id"])
    ]
)
class ProductReviewHelpfulVote(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    val review: ProductReview,

    @Column(name = "user_id", nullable = false)
    val userId: Long,
) : BaseEntity()
