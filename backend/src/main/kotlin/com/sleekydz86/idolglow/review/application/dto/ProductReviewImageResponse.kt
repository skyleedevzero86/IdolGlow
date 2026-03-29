package com.sleekydz86.idolglow.review.application.dto

import com.sleekydz86.idolglow.image.domain.Image
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상품 리뷰 이미지 응답 DTO")
data class ProductReviewImageResponse(
    @Schema(description = "이미지 ID", example = "100")
    val id: Long,
    @Schema(description = "이미지 원본 파일명", example = "review.png")
    val originalFilename: String,
    @Schema(description = "이미지 URL", example = "https://cdn.example.com/reviews/uuid.png")
    val url: String,
    @Schema(description = "정렬 순서", example = "0")
    val sortOrder: Int
) {
    companion object {
        fun from(image: Image): ProductReviewImageResponse =
            ProductReviewImageResponse(
                id = image.id,
                originalFilename = image.originalFilename,
                url = image.url,
                sortOrder = image.sortOrder
            )
    }
}
