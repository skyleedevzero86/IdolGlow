package com.sleekydz86.idolglow.wish.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.wish.application.dto.WishedProductPagingResponse

data class WishProductGraphQlResponse(
    val id: String,
    val name: String,
    val minPrice: String,
    val tagNames: List<String>,
) {
    companion object {
        fun from(response: WishedProductPagingResponse): WishProductGraphQlResponse =
            WishProductGraphQlResponse(
                id = response.id.asGraphQlId(),
                name = response.name,
                minPrice = response.minPrice.asGraphQlNumber(),
                tagNames = response.tagNames
            )
    }
}
