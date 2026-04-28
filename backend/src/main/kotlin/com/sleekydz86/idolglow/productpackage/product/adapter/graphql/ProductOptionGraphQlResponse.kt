package com.sleekydz86.idolglow.productpackage.product.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductOptionResponse

data class ProductOptionGraphQlResponse(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val location: String,
    val imageUrls: List<String>,
) {
    companion object {
        fun from(response: ProductOptionResponse): ProductOptionGraphQlResponse =
            ProductOptionGraphQlResponse(
                id = response.id.asGraphQlId(),
                name = response.name,
                description = response.description,
                price = response.price.asGraphQlNumber(),
                location = response.location,
                imageUrls = response.imageUrls,
            )
    }
}
