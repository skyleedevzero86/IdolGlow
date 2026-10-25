package com.sleekydz86.idolglow.groundedsearch.adapter.web

import java.math.BigDecimal

data class GroundedOptionAnswerResponse(
    val optionId: Long,
    val name: String,
    val price: BigDecimal,
)
