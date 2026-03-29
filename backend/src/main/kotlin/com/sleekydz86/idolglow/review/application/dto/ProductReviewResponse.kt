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
    val images: List<ProductReviewImageResponse> = emptyList(),

    @Schema(description = "방문 완료 예약 기반 인증 리뷰 여부")
    val verifiedPurchase: Boolean = false,

    @Schema(description = "도움돼요 수")
    val helpfulCount: Long = 0L,

    @Schema(description = "비공개(신고 누적 등) 여부 — 본인 마이페이지에서만 true일 수 있음")
    val hidden: Boolean = false,
) {

    companion object {
        fun from(productReview: ProductReview, images: List<Image> = emptyList()): ProductReviewResponse {
            val createdAt = productReview.createdAt ?: LocalDateTime.now()
            return ProductReviewResponse(
                reviewId = productReview.id,
                productId = requireNotNull(productReview.product.id) { "productId 값이 필요합니다." },
                userId = productReview.userId,
                rating = productReview.rating.value,
                content = productReview.content,
                createdAt = createdAt,
                images = images.map { ProductReviewImageResponse.from(it) },
                verifiedPurchase = productReview.verifiedPurchase,
                helpfulCount = productReview.helpfulCount,
                hidden = productReview.isHidden(),
            )
        }
    }
}
