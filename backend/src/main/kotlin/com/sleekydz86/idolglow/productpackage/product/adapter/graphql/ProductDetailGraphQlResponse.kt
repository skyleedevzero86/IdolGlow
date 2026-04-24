package com.sleekydz86.idolglow.productpackage.product.graphql

import com.sleekydz86.idolglow.global.graphql.asGraphQlId
import com.sleekydz86.idolglow.global.graphql.asGraphQlNumber
import com.sleekydz86.idolglow.global.graphql.asGraphQlValue
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse

data class ProductDetailGraphQlResponse(
    val id: String,
    val name: String,
    val description: String,
    val options: List<ProductOptionGraphQlResponse>,
    val tagNames: List<String>,
    val slotStartDate: String?,
    val slotEndDate: String?,
    val slotStartTime: String?,
    val slotEndTime: String?,
    val reservationSlotCount: Int,
    val basePrice: String,
    val optionsTotalPrice: String,
    val minPrice: String,
    val totalPrice: String,
    val location: ProductLocationSummaryGraphQlResponse?,
    val thumbnailUrl: String?,
    val imageUrls: List<String>,
) {
    companion object {
        fun from(response: ProductSpecificResponse): ProductDetailGraphQlResponse =
            ProductDetailGraphQlResponse(
                id = response.id.asGraphQlId(),
                name = response.name,
                description = response.description,
                options = response.options.map(ProductOptionGraphQlResponse::from),
                tagNames = response.tagNames,
                slotStartDate = response.slotStartDate.asGraphQlValue(),
                slotEndDate = response.slotEndDate.asGraphQlValue(),
                slotStartTime = response.slotStartTime.asGraphQlValue(),
                slotEndTime = response.slotEndTime.asGraphQlValue(),
                reservationSlotCount = response.reservationSlotCount,
                basePrice = response.basePrice.asGraphQlNumber(),
                optionsTotalPrice = response.optionsTotalPrice.asGraphQlNumber(),
                minPrice = response.minPrice.asGraphQlNumber(),
                totalPrice = response.totalPrice.asGraphQlNumber(),
                location = response.location?.let { ProductLocationSummaryGraphQlResponse.from(it) },
                thumbnailUrl = response.thumbnailUrl,
                imageUrls = response.imageUrls,
            )
    }
}
