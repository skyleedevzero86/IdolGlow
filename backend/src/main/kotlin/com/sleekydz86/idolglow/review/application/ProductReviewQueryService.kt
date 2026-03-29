package com.sleekydz86.idolglow.review.application

import com.sleekydz86.idolglow.image.domain.ImageRepository
import com.sleekydz86.idolglow.image.domain.vo.ImageAggregateType
import com.sleekydz86.idolglow.review.application.dto.ProductReviewResponse
import com.sleekydz86.idolglow.review.domain.ProductReview
import com.sleekydz86.idolglow.review.domain.ProductReviewRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional(readOnly = true)
@Service
class ProductReviewQueryService(
    private val productReviewRepository: ProductReviewRepository,
    private val imageRepository: ImageRepository
) {

    fun findReviewsByProduct(productId: Long): List<ProductReviewResponse> {
        val reviews = productReviewRepository.findByProductId(productId)
            .sortedByDescending { it.createdAt }
        val reviewIds = reviews.map { it.id }
        val images = imageRepository.findByAggregates(ImageAggregateType.PRODUCT_REVIEW, reviewIds)
        val imageMap = images.groupBy { it.aggregateId }

        return reviews.map { review ->
            ProductReviewResponse.from(review, imageMap[review.id].orEmpty())
        }
    }

    fun toResponse(review: ProductReview): ProductReviewResponse {
        val images = imageRepository.findByAggregate(ImageAggregateType.PRODUCT_REVIEW, review.id)
        return ProductReviewResponse.from(review, images)
    }

    fun findReviewsByUser(userId: Long): List<ProductReviewResponse> {
        val reviews = productReviewRepository.findByUserId(userId)
            .sortedByDescending { it.createdAt }
        val reviewIds = reviews.map { it.id }
        val images = imageRepository.findByAggregates(ImageAggregateType.PRODUCT_REVIEW, reviewIds)
        val imageMap = images.groupBy { it.aggregateId }

        return reviews.map { review ->
            ProductReviewResponse.from(review, imageMap[review.id].orEmpty())
        }
    }
}
