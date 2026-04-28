package com.sleekydz86.idolglow.exchangerate.application

import com.sleekydz86.idolglow.exchangerate.application.port.out.ExchangeRateQueryPort
import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

class ExchangeRateQueryServiceTest {

    private val exchangeRateQueryPort = mockk<ExchangeRateQueryPort>()
    private val service = ExchangeRateQueryService(exchangeRateQueryPort)

    @Test
    fun `returns same day rates when api has data`() {
        val searchDate = LocalDate.of(2026, 4, 24)
        val expected = listOf(
            quote(curUnit = "JPY(100)", dealBasR = "927"),
        )
        every { exchangeRateQueryPort.fetchDailyRates(searchDate) } returns expected

        val result = service.getDailyRates(searchDate)

        assertEquals(expected, result)
    }

    @Test
    fun `retries previous business day when target date has no data`() {
        val monday = LocalDate.of(2026, 4, 27)
        val friday = LocalDate.of(2026, 4, 24)
        val expected = listOf(
            quote(curUnit = "USD", dealBasR = "1480.6"),
        )
        every { exchangeRateQueryPort.fetchDailyRates(monday) } returns emptyList()
        every { exchangeRateQueryPort.fetchDailyRates(friday) } returns expected

        val result = service.getDailyRates(monday)

        assertEquals(expected, result)
    }

    @Test
    fun `continues retrying previous business days until it finds data`() {
        val monday = LocalDate.of(2026, 5, 11)
        val friday = LocalDate.of(2026, 5, 8)
        val thursday = LocalDate.of(2026, 5, 7)
        val expected = listOf(
            quote(curUnit = "CNH", dealBasR = "216.83"),
        )
        every { exchangeRateQueryPort.fetchDailyRates(monday) } returns emptyList()
        every { exchangeRateQueryPort.fetchDailyRates(friday) } returns emptyList()
        every { exchangeRateQueryPort.fetchDailyRates(thursday) } returns expected

        val result = service.getDailyRates(monday)

        assertEquals(expected, result)
    }

    private fun quote(curUnit: String, dealBasR: String): ExchangeRateQuote = ExchangeRateQuote(
        curUnit = curUnit,
        curNm = curUnit,
        ttb = dealBasR,
        tts = dealBasR,
        dealBasR = dealBasR,
        bkpr = dealBasR,
    )
}
