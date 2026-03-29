package com.sleekydz86.idolglow.graphql

data class CreateProductReviewGraphQlInput(
    val reservationId: String,
    val rating: Int,
    val content: String,
)
