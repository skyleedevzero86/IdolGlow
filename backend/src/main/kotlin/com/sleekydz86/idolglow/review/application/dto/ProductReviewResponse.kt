package com.sleekydz86.idolglow.review.application.dto

import com.sleekydz86.idolglow.image.domain.Image
import com.sleekydz86.idolglow.review.domain.ProductReview
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "상품 리뷰 응답 DTO")
data class ProductReviewResponse(
    @Schema(description = "리뷰 ID", example = "21")
    val reviewId: Long,
    @Schema(description = "상품 ID", example = "3")
    val productId: Long,
    @Schema(description = "리뷰 작성자 ID", example = "7")
    val userId: Long,
    @Schema(description = "상품 평점(1~5)", example = "4")
    val rating: Int,
    @Schema(description = "리뷰 내용", example = "What a nice product!")
    val content: String,
    @Schema(description = "리뷰 작성 일시", example = "2025-12-07T10:15:30")
    val createdAt: LocalDateTime,
    @Schema(description = "리뷰 이미지 목록")
    val images: List<ProductReviewImageResponse> = emptyList()
) {

    companion object {
        fun from(productReview: ProductReview, images: List<Image> = emptyList()): ProductReviewResponse {
            val createdAt = productReview.createdAt ?: LocalDateTime.now()
            return ProductReviewResponse(
                reviewId = productReview.id,
                productId = requireNotNull(productReview.product.id) { "productId is required." },
                userId = productReview.userId,
                rating = productReview.rating.value,
                content = productReview.content,
                createdAt = createdAt,
                images = images.map { ProductReviewImageResponse.from(it) }
            )
        }
    }
}
