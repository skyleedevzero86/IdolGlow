package com.sleekydz86.idolglow.groundedsearch.domain

import java.math.BigDecimal
import java.time.LocalDate

data class GroundedFilter(
    val region: String?,
    val dateFrom: LocalDate?,
    val dateTo: LocalDate?,
    val maxBudget: BigDecimal?,
    val category: String?,
) {
    fun widenDates(): GroundedFilter {
        if (dateFrom == null || dateTo == null) {
            return this
        }
        val slack = 2L + 1L
        return copy(dateFrom = dateFrom.minusDays(slack), dateTo = dateTo.plusDays(slack))
    }
}
