package com.sleekydz86.idolglow.exchange.adapter.web.dto

import java.math.BigDecimal

data class ExchangeBranchResponse(
    val branchId: Long,
    val name: String,
    val rate: BigDecimal,
    val currency: String,
    val lat: Double,
    val lng: Double,
    val airportHub: Boolean,
    val durationMinutesFromAirport: Int?,
)
