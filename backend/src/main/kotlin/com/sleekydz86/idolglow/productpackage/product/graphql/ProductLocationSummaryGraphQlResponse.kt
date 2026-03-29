package com.sleekydz86.idolglow.productpackage.product.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductLocationSummaryResponse

data class ProductLocationSummaryGraphQlResponse(
    val name: String,
    val latitude: String,
    val longitude: String,
    val roadAddressName: String?,
    val addressName: String?,
    val displayAddress: String,
) {
    companion object {
        fun from(dto: ProductLocationSummaryResponse): ProductLocationSummaryGraphQlResponse =
            ProductLocationSummaryGraphQlResponse(
                name = dto.name,
                latitude = dto.latitude.asGraphQlNumber(),
                longitude = dto.longitude.asGraphQlNumber(),
                roadAddressName = dto.roadAddressName,
                addressName = dto.addressName,
                displayAddress = dto.displayAddress,
            )
    }
}
