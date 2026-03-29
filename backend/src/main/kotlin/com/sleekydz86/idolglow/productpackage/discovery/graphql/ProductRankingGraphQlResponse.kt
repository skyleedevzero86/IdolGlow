package com.sleekydz86.idolglow.productpackage.discovery.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.productpackage.discovery.application.dto.ProductRankingResponse

data class ProductRankingGraphQlResponse(
    val id: String,
    val name: String,
    val description: String,
    val minPrice: String,
    val totalPrice: String,
    val tagNames: List<String>,
    val wishCount: Int,
    val averageRating: String,
    val reviewCount: Int,
    val matchedTags: List<String>,
    val thumbnailUrl: String?,
) {
    companion object {
        fun from(response: ProductRankingResponse): ProductRankingGraphQlResponse =
            ProductRankingGraphQlResponse(
                id = response.id.asGraphQlId(),
                name = response.name,
                description = response.description,
                minPrice = response.minPrice.asGraphQlNumber(),
                totalPrice = response.totalPrice.asGraphQlNumber(),
                tagNames = response.tagNames,
                wishCount = response.wishCount.toInt(),
                averageRating = response.averageRating.asGraphQlNumber(),
                reviewCount = response.reviewCount.toInt(),
                matchedTags = response.matchedTags,
                thumbnailUrl = response.thumbnailUrl,
            )
    }
}
