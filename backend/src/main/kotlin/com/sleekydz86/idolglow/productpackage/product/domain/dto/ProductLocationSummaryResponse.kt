package com.sleekydz86.idolglow.productpackage.product.domain.dto

import com.sleekydz86.idolglow.productpackage.product.domain.ProductLocation
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "상품 대표 위치(지도용)")
data class ProductLocationSummaryResponse(
    @field:Schema(description = "장소 표시명")
    val name: String,

    @field:Schema(description = "위도")
    val latitude: BigDecimal,

    @field:Schema(description = "경도")
    val longitude: BigDecimal,

    @field:Schema(description = "도로명 주소")
    val roadAddressName: String?,

    @field:Schema(description = "지번 주소")
    val addressName: String?,

    @field:Schema(description = "표시용 주소")
    val displayAddress: String,
) {
    companion object {
        fun from(location: ProductLocation): ProductLocationSummaryResponse =
            ProductLocationSummaryResponse(
                name = location.name,
                latitude = location.latitude,
                longitude = location.longitude,
                roadAddressName = location.roadAddressName,
                addressName = location.addressName,
                displayAddress = location.displayAddress(),
            )
    }
}
