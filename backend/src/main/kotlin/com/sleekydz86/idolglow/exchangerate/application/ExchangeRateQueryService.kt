package com.sleekydz86.idolglow.exchangerate.application

import com.sleekydz86.idolglow.exchangerate.application.port.out.ExchangeRateQueryPort
import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class ExchangeRateQueryService(
    private val exchangeRateQueryPort: ExchangeRateQueryPort,
) {
    fun getDailyRates(searchDate: LocalDate?): List<ExchangeRateQuote> =
        exchangeRateQueryPort.fetchDailyRates(searchDate)
}
