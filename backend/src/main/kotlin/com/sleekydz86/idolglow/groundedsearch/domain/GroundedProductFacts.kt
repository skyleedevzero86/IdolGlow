package com.sleekydz86.idolglow.groundedsearch.domain

import java.math.BigDecimal

data class GroundedProductFacts(
    val productId: Long,
    val name: String,
    val minPrice: BigDecimal,
    val options: List<GroundedOptionPrice>,
    val slots: List<GroundedSlotFact>,
)
