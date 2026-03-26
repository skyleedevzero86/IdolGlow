package com.sleekydz86.idolglow.productpackage.product.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse

data class ProductSummaryGraphQlResponse(
    val id: String,
    val name: String,
    val description: String,
    val minPrice: String,
    val totalPrice: String,
    val tagNames: List<String>,
) {
    companion object {
        fun from(response: ProductPagingQueryResponse): ProductSummaryGraphQlResponse =
            ProductSummaryGraphQlResponse(
                id = response.id.asGraphQlId(),
                name = response.name,
                description = response.description,
                minPrice = response.minPrice.asGraphQlNumber(),
                totalPrice = response.totalPrice.asGraphQlNumber(),
                tagNames = response.tagNames
            )
    }
}
