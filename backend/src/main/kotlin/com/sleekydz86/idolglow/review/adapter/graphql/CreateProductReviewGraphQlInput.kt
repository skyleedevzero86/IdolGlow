package com.sleekydz86.idolglow.review.adapter.graphql

data class CreateProductReviewGraphQlInput(
    val reservationId: String,
    val rating: Int,
    val content: String,
)
