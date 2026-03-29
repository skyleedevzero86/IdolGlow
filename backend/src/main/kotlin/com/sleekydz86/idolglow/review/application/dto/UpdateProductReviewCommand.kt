package com.sleekydz86.idolglow.review.application.dto

data class UpdateProductReviewCommand(
    val productId: Long,
    val reviewId: Long,
    val userId: Long,
    val rating: Int,
    val content: String
)
