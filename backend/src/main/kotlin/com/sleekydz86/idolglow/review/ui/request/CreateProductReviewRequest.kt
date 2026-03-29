package com.sleekydz86.idolglow.review.ui.request

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상품 리뷰 생성 요청 DTO")
data class CreateProductReviewRequest(
    @field:Min(1)
    @field:Max(5)
    @field:Schema(description = "상품 평점(1~5)", example = "4")
    val rating: Int,

    @field:NotBlank
    @field:Size(max = 2000)
    @field:Schema(description = "리뷰 내용", example = "상품 구성이 좋았습니다.")
    val content: String
)