package com.sleekydz86.idolglow.review.infrastructure

import com.sleekydz86.idolglow.review.domain.ProductReviewHelpfulVote
import org.springframework.data.jpa.repository.JpaRepository

interface ProductReviewHelpfulVoteJpaRepository : JpaRepository<ProductReviewHelpfulVote, Long> {
    fun findByReviewIdAndUserId(reviewId: Long, userId: Long): ProductReviewHelpfulVote?
    fun deleteByReviewIdAndUserId(reviewId: Long, userId: Long): Long
}
