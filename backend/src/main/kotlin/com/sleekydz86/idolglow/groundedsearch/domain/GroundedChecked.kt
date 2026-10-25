package com.sleekydz86.idolglow.groundedsearch.domain

import java.math.BigDecimal
import java.time.LocalDate

data class GroundedChecked(
    val region: String?,
    val dateFrom: LocalDate?,
    val dateTo: LocalDate?,
    val maxBudget: BigDecimal?,
    val category: String?,
    val people: Int?,
    val ignored: List<String>,
) {
    fun hasConstraint(): Boolean = region != null || dateFrom != null || maxBudget != null || category != null

    fun toFilter(): GroundedFilter = GroundedFilter(region, dateFrom, dateTo, maxBudget, category)
}
