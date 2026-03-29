package com.sleekydz86.idolglow.productpackage.product.application.event

import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductLocationPayload

data class ProductCreateEvent(
    val productId: Long,
    val location: ProductLocationPayload? = null
)
