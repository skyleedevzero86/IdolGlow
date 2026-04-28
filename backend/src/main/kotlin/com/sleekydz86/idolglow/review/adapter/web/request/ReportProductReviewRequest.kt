package com.sleekydz86.idolglow.review.ui.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

@Schema(description = "리뷰 신고 요청")
data class ReportProductReviewRequest(
    @field:NotBlank
    @field:Size(max = 200)
    @field:Schema(description = "신고 사유", example = "스팸/광고성 내용")
    val reason: String,
)
