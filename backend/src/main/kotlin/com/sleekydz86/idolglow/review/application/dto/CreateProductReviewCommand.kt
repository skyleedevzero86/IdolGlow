package com.sleekydz86.idolglow.review.application.dto

data class CreateProductReviewCommand(
    val productId: Long,
    val userId: Long,
    val reservationId: Long,
    val rating: Int,
    val content: String,
)
