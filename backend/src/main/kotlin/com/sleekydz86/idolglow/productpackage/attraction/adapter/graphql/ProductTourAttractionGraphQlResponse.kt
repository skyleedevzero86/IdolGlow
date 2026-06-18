package com.sleekydz86.idolglow.productpackage.attraction.adapter.graphql

import com.sleekydz86.idolglow.global.adapter.graphql.asGraphQlId
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
