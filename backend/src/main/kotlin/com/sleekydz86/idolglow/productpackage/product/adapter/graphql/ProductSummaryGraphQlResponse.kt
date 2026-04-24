package com.sleekydz86.idolglow.productpackage.product.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import java.math.BigDecimal

data class ProductSummaryGraphQlResponse(
    val id: String,
    val name: String,
    val description: String,
    val basePrice: String,
    val optionsTotalPrice: String,
    val minPrice: String,
    val totalPrice: String,
    val tagNames: List<String>,
    val location: ProductLocationSummaryGraphQlResponse?,
    val distanceMeters: String?,
    val wishCount: Int,
    val averageRating: String,
    val reviewCount: Int,
    val thumbnailUrl: String?,
    val tourAttractionPickCount: Int,
) {
    companion object {
        fun from(response: ProductPagingQueryResponse): ProductSummaryGraphQlResponse =
            ProductSummaryGraphQlResponse(
                id = response.id.asGraphQlId(),
                name = response.name,
                description = response.description,
                basePrice = response.basePrice.asGraphQlNumber(),
                optionsTotalPrice = response.optionsTotalPrice.asGraphQlNumber(),
                minPrice = response.minPrice.asGraphQlNumber(),
                totalPrice = response.totalPrice.asGraphQlNumber(),
                tagNames = response.tagNames,
                location = response.location?.let { ProductLocationSummaryGraphQlResponse.from(it) },
                distanceMeters = response.distanceMeters?.let { d ->
                    BigDecimal.valueOf(d).stripTrailingZeros().toPlainString()
                },
                wishCount = response.wishCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                averageRating = BigDecimal.valueOf(response.averageRating).asGraphQlNumber(),
                reviewCount = response.reviewCount.coerceAtMost(Int.MAX_VALUE.toLong()).toInt(),
                thumbnailUrl = response.thumbnailUrl,
                tourAttractionPickCount = response.tourAttractionPickCount,
            )
    }
}
