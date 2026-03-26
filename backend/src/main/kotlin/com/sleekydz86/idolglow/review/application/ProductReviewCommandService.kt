package com.sleekydz86.idolglow.review.application

import com.sleekydz86.idolglow.image.application.ImageEventPublisher
import jakarta.persistence.EntityNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class ProductReviewCommandService(
    private val productRepository: ProductRepository,
    private val productReviewRepository: ProductReviewRepository,
    private val imageEventPublisher: ImageEventPublisher,
) {

    fun createReview(
        command: CreateProductReviewCommand,
        images: List<ReviewImageFile> = emptyList()
    ): ProductReview {
        val product = loadProduct(command.productId)
        ensureNotReviewed(product.id, command.userId)

        val review = ProductReview(
            product = product,
            userId = command.userId,
            rating = ReviewRating.of(command.rating),
            content = command.content
        )

        val productReview = productReviewRepository.save(review)
        storeImages(productReview.id, images)
        return productReview
    }

    private fun loadProduct(productId: Long): Product {
        return productRepository.findById(productId)
            ?: throw EntityNotFoundException("Product $productId does not exist.")
    }

    private fun ensureNotReviewed(productId: Long, userId: Long) {
        val existing = productReviewRepository.findByProductIdAndUserId(productId, userId)
        require(existing == null) { "You already wrote a review for this product." }
    }

    fun updateReview(
        command: UpdateProductReviewCommand,
        images: List<ReviewImageFile>? = null
    ): ProductReview {
        val review = findProductByReviewId(command.reviewId)
        review.validateOwner(userId = command.userId, productId = command.productId)
        review.changeReview(
            rating = ReviewRating.of(command.rating),
            content = command.content
        )

        images?.let {
            deleteImages(review.id)
            storeImages(review.id, it)
        }
        return review
    }

    private fun findProductByReviewId(reviewId: Long) =
        productReviewRepository.findById(reviewId)
            ?: throw IllegalArgumentException("Review $reviewId not found.")

    fun deleteReview(productId: Long, reviewId: Long, userId: Long) {
        val review = findProductByReviewId(reviewId)
        review.validateOwner(userId = userId, productId = productId)
        deleteImages(reviewId)
        productReviewRepository.delete(review)
    }

    private fun storeImages(reviewId: Long, images: List<ReviewImageFile>) {
        images.forEach { image ->
            imageEventPublisher.publishCreate(
                aggregateType = ImageAggregateType.PRODUCT_REVIEW,
                aggregateId = reviewId,
                originalFilename = image.originalFilename,
                content = image.content,
                sortOrder = image.sortOrder
            )
        }
    }

    private fun deleteImages(reviewId: Long) {
        imageEventPublisher.publishDelete(
            aggregateType = ImageAggregateType.PRODUCT_REVIEW,
            aggregateId = reviewId,
        )
    }
}
