package com.sleekydz86.idolglow.groundedsearch.adapter.web

import com.sleekydz86.idolglow.groundedsearch.domain.GroundedChecked
import java.math.BigDecimal
import java.time.LocalDate

data class GroundedExtractedResponse(
    val region: String?,
    val dateFrom: LocalDate?,
    val dateTo: LocalDate?,
    val maxBudget: BigDecimal?,
    val category: String?,
    val people: Int?,
) {
    companion object {
        fun from(checked: GroundedChecked): GroundedExtractedResponse =
            GroundedExtractedResponse(
                region = checked.region,
                dateFrom = checked.dateFrom,
                dateTo = checked.dateTo,
                maxBudget = checked.maxBudget,
                category = checked.category,
                people = checked.people,
            )
    }
}
