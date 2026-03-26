package com.sleekydz86.idolglow.review.infrastructure

import com.sleekydz86.idolglow.review.domain.ProductReview
import com.sleekydz86.idolglow.review.domain.ProductReviewRepository
import org.springframework.stereotype.Repository

@Repository
class ProductReviewRepositoryImpl(
    private val productReviewJpaRepository: ProductReviewJpaRepository
) : ProductReviewRepository {

    override fun save(review: ProductReview): ProductReview =
        productReviewJpaRepository.save(review)

    override fun findById(reviewId: Long): ProductReview? =
        productReviewJpaRepository.findById(reviewId).orElse(null)

    override fun findByProductId(productId: Long): List<ProductReview> =
        productReviewJpaRepository.findAllByProductId(productId)

    override fun findByProductIdAndUserId(productId: Long, userId: Long): ProductReview? =
        productReviewJpaRepository.findByProductIdAndUserId(productId, userId)

    override fun findByUserId(userId: Long): List<ProductReview> =
        productReviewJpaRepository.findAllByUserId(userId)

    override fun delete(review: ProductReview) =
        productReviewJpaRepository.delete(review)
}
