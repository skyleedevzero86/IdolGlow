package com.sleekydz86.idolglow.review.application.dto

data class ReviewImageFile(
    val originalFilename: String,
    val content: ByteArray,
    val sortOrder: Int
)
