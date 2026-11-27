package com.sleekydz86.idolglow.productpackage.product.application.dto

import com.sleekydz86.idolglow.productpackage.product.domain.ProductEventPriceType
import java.math.BigDecimal
import java.time.LocalDate

data class ProductEventSaleInput(
    val startsOn: LocalDate?,
    val endsOn: LocalDate?,
    val priceType: ProductEventPriceType?,
    val discountRate: Int?,
    val eventBasePrice: BigDecimal?,
)
