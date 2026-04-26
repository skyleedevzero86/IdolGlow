package com.sleekydz86.idolglow.exchangerate.application

import com.sleekydz86.idolglow.exchangerate.application.port.out.ExchangeRateQueryPort
import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import com.sleekydz86.idolglow.exchange.infrastructure.ExchangeBranchJpaRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Transactional(readOnly = true)
@Service
class ExchangeRateQueryService(
    private val exchangeRateQueryPort: ExchangeRateQueryPort,
    private val exchangeBranchJpaRepository: ExchangeBranchJpaRepository,
) {
    fun getDailyRates(searchDate: LocalDate?): List<ExchangeRateQuote> {
        val official = exchangeRateQueryPort.fetchDailyRates(searchDate)
        if (official.isNotEmpty()) {
            return official
        }
        return fallbackBranchRates()
    }

    private fun fallbackBranchRates(): List<ExchangeRateQuote> =
        exchangeBranchJpaRepository.findAll()
            .sortedWith(compareBy({ it.currency }, { it.sortOrder }))
            .groupBy { it.currency.uppercase() }
            .mapNotNull { (currency, rows) ->
                val hub = rows.firstOrNull { it.airportHub } ?: rows.firstOrNull() ?: return@mapNotNull null
                ExchangeRateQuote(
                    curUnit = currency,
                    curNm = FALLBACK_CURRENCY_NAMES[currency] ?: currency,
                    ttb = hub.rate.stripTrailingZeros().toPlainString(),
                    tts = hub.rate.stripTrailingZeros().toPlainString(),
                    dealBasR = hub.rate.stripTrailingZeros().toPlainString(),
                    bkpr = hub.rate.stripTrailingZeros().toPlainString(),
                )
            }
            .sortedBy { FALLBACK_ORDER.indexOf(it.curUnit).let { idx -> if (idx >= 0) idx else Int.MAX_VALUE } }

    companion object {
        private val FALLBACK_ORDER = listOf("CNY", "EUR", "JPY", "USD")

        private val FALLBACK_CURRENCY_NAMES = mapOf(
            "CNY" to "위안",
            "EUR" to "유로",
            "JPY" to "엔",
            "USD" to "달러",
        )
    }
}
