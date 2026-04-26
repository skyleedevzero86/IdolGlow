package com.sleekydz86.idolglow.exchange.adapter.web.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

data class CreateExchangeAlertRequest(
    @field:NotBlank
    val fromCurrency: String,
    @field:NotBlank
    val toCurrency: String,
    @field:NotNull
    @field:Positive
    val targetRate: BigDecimal,
)

data class ExchangeAlertCreatedResponse(
    val id: Long,
)
