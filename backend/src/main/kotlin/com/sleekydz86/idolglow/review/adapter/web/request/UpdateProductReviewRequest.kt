package com.sleekydz86.idolglow.review.ui.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상품 리뷰 수정 요청 DTO")
data class UpdateProductReviewRequest(
    @field:Min(1)
    @field:Max(5)
    @field:Schema(description = "상품 평점(1~5)", example = "5")
    val rating: Int,

    @field:NotBlank
    @field:Size(max = 2000)
    @field:Schema(description = "수정된 리뷰 내용", example = "상품 이용해서 멋있어졌습니다.")
    val content: String
)
