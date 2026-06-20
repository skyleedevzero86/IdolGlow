package com.sleekydz86.idolglow.productpackage.product.adapter.graphql

data class ProductSliceGraphQlResponse(
    val items: List<ProductSummaryGraphQlResponse>,
    val nextCursor: String?,
    val nextOffset: Int?,
)
