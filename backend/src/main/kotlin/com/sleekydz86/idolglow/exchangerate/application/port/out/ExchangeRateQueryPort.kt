package com.sleekydz86.idolglow.exchangerate.application.port.out

import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import java.time.LocalDate

fun interface ExchangeRateQueryPort {
    fun fetchDailyRates(searchDate: LocalDate?): List<ExchangeRateQuote>
}
