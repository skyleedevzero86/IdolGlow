package com.sleekydz86.idolglow.review.domain

interface ProductReviewRepository {
    fun save(review: ProductReview): ProductReview
    fun findById(reviewId: Long): ProductReview?
    fun findByProductId(productId: Long): List<ProductReview>
    fun findByProductIdAndUserId(productId: Long, userId: Long): ProductReview?
    fun findByUserId(userId: Long): List<ProductReview>
    fun existsByReservationId(reservationId: Long): Boolean
    fun delete(review: ProductReview)
}