package com.sleekydz86.idolglow.review.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlId
import com.sleekydz86.idolglow.review.application.dto.ProductReviewImageResponse

data class ProductReviewImageGraphQlResponse(
    val id: String,
    val originalFilename: String,
    val url: String,
    val sortOrder: Int,
) {
    companion object {
        fun from(response: ProductReviewImageResponse): ProductReviewImageGraphQlResponse =
            ProductReviewImageGraphQlResponse(
                id = response.id.asGraphQlId(),
                originalFilename = response.originalFilename,
                url = response.url,
                sortOrder = response.sortOrder,
            )
    }
}
