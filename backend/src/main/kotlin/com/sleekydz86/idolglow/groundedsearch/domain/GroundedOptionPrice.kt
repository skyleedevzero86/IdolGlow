package com.sleekydz86.idolglow.groundedsearch.domain

import java.math.BigDecimal

data class GroundedOptionPrice(
    val optionId: Long,
    val name: String,
    val price: BigDecimal,
)
