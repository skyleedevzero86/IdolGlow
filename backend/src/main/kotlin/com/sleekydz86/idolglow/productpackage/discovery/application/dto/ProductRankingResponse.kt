package com.sleekydz86.idolglow.productpackage.discovery.application.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.math.RoundingMode

data class ProductRankingResponse(
    @field:Schema(description = "Product id", example = "1")
    val id: Long,
    @field:Schema(description = "Product name", example = "Hair and makeup package")
    val name: String,
    @field:Schema(description = "Product description", example = "Hair and makeup course included")
    val description: String,
    @field:Schema(description = "Minimum price", example = "100000.00")
    val minPrice: BigDecimal,
    @field:Schema(description = "Total price", example = "300000.00")
    val totalPrice: BigDecimal,
    @field:Schema(description = "Tag names", example = "[\"idol\", \"makeup\"]")
    val tagNames: List<String>,
    @field:Schema(description = "Wish count", example = "120")
    val wishCount: Long,
    @field:Schema(description = "Average rating", example = "4.70")
    val averageRating: BigDecimal,
    @field:Schema(description = "Review count", example = "42")
    val reviewCount: Long,
    @field:Schema(description = "Matched tags for recommendation", example = "[\"idol\", \"makeup\"]")
    val matchedTags: List<String>,
) {
    companion object {
        fun from(
            product: Product,
            tagNames: List<String>,
            wishCount: Long,
            averageRating: Double,
            reviewCount: Long,
            matchedTags: List<String> = emptyList(),
        ): ProductRankingResponse =
            ProductRankingResponse(
                id = product.id,
                name = product.name,
                description = product.description,
                minPrice = product.minPrice,
                totalPrice = product.totalPrice,
                tagNames = tagNames,
                wishCount = wishCount,
                averageRating = BigDecimal.valueOf(averageRating).setScale(2, RoundingMode.HALF_UP),
                reviewCount = reviewCount,
                matchedTags = matchedTags
            )
    }
}
