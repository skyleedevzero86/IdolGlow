package com.sleekydz86.idolglow.exchange.adapter.web.dto

import com.sleekydz86.idolglow.exchange.application.dto.CreateExchangeAlertCommand
import com.sleekydz86.idolglow.exchange.application.dto.ExchangeBranchResult

fun ExchangeBranchResult.toWebResponse(): ExchangeBranchResponse =
    ExchangeBranchResponse(
        branchId = branchId,
        name = name,
        rate = rate,
        currency = currency,
        lat = lat,
        lng = lng,
        airportHub = airportHub,
        durationMinutesFromAirport = durationMinutesFromAirport,
    )

fun CreateExchangeAlertRequest.toCommand(): CreateExchangeAlertCommand =
    CreateExchangeAlertCommand(
        fromCurrency = fromCurrency,
        toCurrency = toCurrency,
        targetRate = targetRate,
    )
