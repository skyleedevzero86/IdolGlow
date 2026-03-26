package com.sleekydz86.idolglow.productpackage.product.application.dto

import java.math.BigDecimal

data class ProductLocationPayload(
    val name: String,
    val latitude: BigDecimal,
    val longitude: BigDecimal,
    val roadAddressName: String?,
    val addressName: String?,
    val kakaoPlaceId: String,
)