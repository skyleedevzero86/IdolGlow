package com.sleekydz86.idolglow.review.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.review.application.dto.ProductReviewResponse

data class ProductReviewGraphQlResponse(
    val reviewId: String,
    val productId: String,
    val userId: String,
    val rating: Int,
    val content: String,
    val createdAt: String,
    val images: List<ProductReviewImageGraphQlResponse>,
    val verifiedPurchase: Boolean,
    val helpfulCount: Int,
    val hidden: Boolean,
    val previewImageUrl: String?,
) {
    companion object {
        fun from(response: ProductReviewResponse): ProductReviewGraphQlResponse =
            ProductReviewGraphQlResponse(
                reviewId = response.reviewId.asGraphQlId(),
                productId = response.productId.asGraphQlId(),
                userId = response.userId.asGraphQlId(),
                rating = response.rating,
                content = response.content,
                createdAt = response.createdAt.toString(),
                images = response.images.map(ProductReviewImageGraphQlResponse::from),
                verifiedPurchase = response.verifiedPurchase,
                helpfulCount = response.helpfulCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                hidden = response.hidden,
                previewImageUrl = response.images.firstOrNull()?.url,
            )
    }
}
