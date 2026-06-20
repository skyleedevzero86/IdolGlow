package com.sleekydz86.idolglow.exchange.application.dto

import java.math.BigDecimal

data class ExchangeBranchResult(
    val branchId: Long,
    val name: String,
    val rate: BigDecimal,
    val currency: String,
    val lat: Double,
    val lng: Double,
    val airportHub: Boolean,
    val durationMinutesFromAirport: Int?,
)

data class CreateExchangeAlertCommand(
    val fromCurrency: String,
    val toCurrency: String,
    val targetRate: BigDecimal,
)
