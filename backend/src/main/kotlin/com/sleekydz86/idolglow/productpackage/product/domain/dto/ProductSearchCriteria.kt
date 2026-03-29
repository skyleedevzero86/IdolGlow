package com.sleekydz86.idolglow.productpackage.product.domain.dto

import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime

data class ProductSearchCriteria(
    val lastId: Long?,
    val offset: Int,
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
    val now: LocalDateTime,
    val today: LocalDate,
) {
    val effectiveTagNames: List<String> = buildList {
        tag?.trim()?.takeIf { it.isNotEmpty() }?.let { add(it) }
        addAll(tags.map { it.trim() }.filter { it.isNotEmpty() }.distinct())
    }.distinct()

    val needsLocationJoin: Boolean =
        nearLatitude != null && nearLongitude != null

    val applyRadiusFilter: Boolean =
        needsLocationJoin && radiusMeters != null && radiusMeters > 0
}
