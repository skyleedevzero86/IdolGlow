package com.sleekydz86.idolglow.productpackage.product.application.event

data class ProductCreateEvent(
    val productId: Long,
    val location: ProductLocationPayload? = null
)
