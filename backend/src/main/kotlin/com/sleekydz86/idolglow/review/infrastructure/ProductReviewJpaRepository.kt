package com.sleekydz86.idolglow.review.infrastructure

import com.sleekydz86.idolglow.review.domain.ProductReview
import org.springframework.data.jpa.repository.JpaRepository

interface ProductReviewJpaRepository : JpaRepository<ProductReview, Long> {
    fun findAllByProductId(productId: Long): List<ProductReview>
    fun findByProductIdAndUserId(productId: Long, userId: Long): ProductReview?
    fun findAllByUserId(userId: Long): List<ProductReview>
}
