package com.sleekydz86.idolglow.productpackage.attraction.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.productpackage.attraction.domain.dto.ProductTourAttractionItemResponse
import com.sleekydz86.idolglow.productpackage.attraction.domain.dto.ProductTourAttractionResponse

data class ProductTourAttractionGraphQlResponse(
    val productId: String,
    val productName: String,
    val district: String,
    val areaCode: Int,
    val signguCode: Int,
    val baseYm: String,
    val attractions: List<ProductTourAttractionItemGraphQlResponse>,
) {
    companion object {
        fun from(response: ProductTourAttractionResponse): ProductTourAttractionGraphQlResponse =
            ProductTourAttractionGraphQlResponse(
                productId = response.productId.asGraphQlId(),
                productName = response.productName,
                district = response.district,
                areaCode = response.areaCode,
                signguCode = response.signguCode,
                baseYm = response.baseYm,
                attractions = response.attractions.map(ProductTourAttractionItemGraphQlResponse::from),
            )
    }
}

data class ProductTourAttractionItemGraphQlResponse(
    val attractionCode: String,
    val name: String,
    val areaName: String?,
    val signguName: String?,
    val categoryLarge: String?,
    val categoryMiddle: String?,
    val rank: Int,
    val mapX: Double?,
    val mapY: Double?,
    val score: Int,
    val reason: String,
) {
    companion object {
        fun from(response: ProductTourAttractionItemResponse): ProductTourAttractionItemGraphQlResponse =
            ProductTourAttractionItemGraphQlResponse(
                attractionCode = response.attractionCode,
                name = response.name,
                areaName = response.areaName,
                signguName = response.signguName,
                categoryLarge = response.categoryLarge,
                categoryMiddle = response.categoryMiddle,
                rank = response.rank,
                mapX = response.mapX,
                mapY = response.mapY,
                score = response.score,
                reason = response.reason,
            )
    }
}
