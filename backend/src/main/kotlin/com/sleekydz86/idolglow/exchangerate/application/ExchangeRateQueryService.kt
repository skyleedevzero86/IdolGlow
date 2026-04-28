package com.sleekydz86.idolglow.exchangerate.application

import com.sleekydz86.idolglow.exchangerate.application.port.out.ExchangeRateQueryPort
import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId

@Transactional(readOnly = true)
@Service
class ExchangeRateQueryService(
    private val exchangeRateQueryPort: ExchangeRateQueryPort,
) {
    fun getDailyRates(searchDate: LocalDate?): List<ExchangeRateQuote> {
        val baseDate = searchDate ?: LocalDate.now(KOREA_ZONE_ID)
        val sameDay = exchangeRateQueryPort.fetchDailyRates(baseDate)
        if (sameDay.isNotEmpty()) {
            return sameDay
        }

        var candidate = baseDate
        repeat(MAX_PREVIOUS_BUSINESS_DAY_RETRIES) {
            candidate = previousBusinessDay(candidate)
            val fallback = exchangeRateQueryPort.fetchDailyRates(candidate)
            if (fallback.isNotEmpty()) {
                return fallback
            }
        }

        return emptyList()
    }

    private fun previousBusinessDay(date: LocalDate): LocalDate {
        var candidate = date.minusDays(1)
        while (candidate.dayOfWeek.value >= 6) {
            candidate = candidate.minusDays(1)
        }
        return candidate
    }

    companion object {
        private val KOREA_ZONE_ID: ZoneId = ZoneId.of("Asia/Seoul")
        private const val MAX_PREVIOUS_BUSINESS_DAY_RETRIES = 7
    }
}
