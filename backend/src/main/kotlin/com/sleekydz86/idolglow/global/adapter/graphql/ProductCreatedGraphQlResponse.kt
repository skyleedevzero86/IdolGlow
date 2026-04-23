package com.sleekydz86.idolglow.global.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId

data class ProductCreatedGraphQlResponse(
    val id: String,
) {
    companion object {
        fun from(productId: Long): ProductCreatedGraphQlResponse =
            ProductCreatedGraphQlResponse(id = productId.asGraphQlId())
    }
}
