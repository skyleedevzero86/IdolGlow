package com.sleekydz86.idolglow.productpackage.option.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.productpackage.option.application.dto.OptionResponse

data class OptionGraphQlResponse(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val location: String,
) {
    companion object {
        fun from(response: OptionResponse): OptionGraphQlResponse =
            OptionGraphQlResponse(
                id = response.id.asGraphQlId(),
                name = response.name,
                description = response.description,
                price = response.price.asGraphQlNumber(),
                location = response.location
            )
    }
}
