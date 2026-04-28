package com.sleekydz86.idolglow.global.graphql

data class CreateProductReviewGraphQlInput(
    val reservationId: String,
    val rating: Int,
    val content: String,
)
