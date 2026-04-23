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

data class AdminProductReviewSummaryResponse(
    val reviewId: Long,
    val productId: Long,
    val productName: String,
    val userId: Long,
    val rating: Int,
    val content: String,
    val createdAt: LocalDateTime,
    val hidden: Boolean,
    val hiddenReason: String?,
    val helpfulCount: Long,
    val images: List<ProductReviewImageResponse>,
)
