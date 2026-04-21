package com.sleekydz86.idolglow.review.infrastructure

import com.sleekydz86.idolglow.review.domain.ProductReview
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.JpaSpecificationExecutor

interface ProductReviewJpaRepository :
    JpaRepository<ProductReview, Long>,
    JpaSpecificationExecutor<ProductReview> {
    fun findAllByProductId(productId: Long): List<ProductReview>
    fun findByProductIdAndUserId(productId: Long, userId: Long): ProductReview?
    fun findAllByUserId(userId: Long): List<ProductReview>
    fun existsByReservationId(reservationId: Long): Boolean
}
