package com.sleekydz86.idolglow.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.wish.application.dto.WishToggleResponse

data class WishToggleGraphQlResponse(
    val id: String,
    val wished: Boolean,
) {
    companion object {
        fun from(response: WishToggleResponse): WishToggleGraphQlResponse =
            WishToggleGraphQlResponse(
                id = response.id.asGraphQlId(),
                wished = response.wished
            )
    }
}
