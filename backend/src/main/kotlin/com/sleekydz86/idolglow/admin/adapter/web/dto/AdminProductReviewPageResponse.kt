package com.sleekydz86.idolglow.admin.ui.dto

import com.sleekydz86.idolglow.review.application.dto.ProductReviewImageResponse
import java.time.LocalDateTime

data class AdminProductReviewPageResponse(
    val reviews: List<AdminProductReviewSummaryResponse>,
    val page: Int,
    val size: Int,
    val totalElements: Long,
    val totalPages: Int,
    val hasNext: Boolean,
)
