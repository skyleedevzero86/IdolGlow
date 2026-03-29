package com.sleekydz86.idolglow.productpackage.product.graphql

data class ProductSliceGraphQlResponse(
    val items: List<ProductSummaryGraphQlResponse>,
    val nextCursor: String?,
)
