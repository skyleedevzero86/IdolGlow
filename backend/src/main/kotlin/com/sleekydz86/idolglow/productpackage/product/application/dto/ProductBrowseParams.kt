package com.sleekydz86.idolglow.productpackage.product.application.dto

import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import java.math.BigDecimal
import java.time.LocalDate

data class ProductBrowseParams(
    val lastId: Long?,
    val offset: Int?,
    val size: Int,
    val tag: String?,
    val tags: List<String>,
    val keyword: String?,
    val minPrice: BigDecimal?,
    val maxPrice: BigDecimal?,
    val visitDate: LocalDate?,
    val reservableOnly: Boolean,
    val sort: ProductSort,
    val nearLatitude: BigDecimal?,
    val nearLongitude: BigDecimal?,
    val radiusMeters: Int?,
)
