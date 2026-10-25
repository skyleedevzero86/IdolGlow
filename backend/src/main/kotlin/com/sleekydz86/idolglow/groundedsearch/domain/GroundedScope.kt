package com.sleekydz86.idolglow.groundedsearch.domain

import java.time.LocalDate

data class GroundedScope(
    val productIds: Set<Long>?,
    val optionToProduct: Map<Long, Long>,
    val relaxed: List<String>,
    val dateFrom: LocalDate?,
    val dateTo: LocalDate?,
    val askConfirm: Boolean,
) {
    companion object {
        fun unfiltered(): GroundedScope = GroundedScope(null, emptyMap(), emptyList(), null, null, false)
    }
}
