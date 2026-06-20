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
    fun `조회일_데이터가_있으면_해당_일자_환율을_반환한다`() {
        // given
        val searchDate = LocalDate.of(2026, 4, 24)
        val expected =
            listOf(
                환율(curUnit = "JPY(100)", dealBasR = "927"),
            )
        every { exchangeRateQueryPort.fetchDailyRates(searchDate) } returns expected

        // when
        val result = service.getDailyRates(searchDate)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `조회일_데이터가_없으면_이전_영업일_환율을_조회한다`() {
        // given
        val monday = LocalDate.of(2026, 4, 27)
        val friday = LocalDate.of(2026, 4, 24)
        val expected =
            listOf(
                환율(curUnit = "USD", dealBasR = "1480.6"),
            )
        every { exchangeRateQueryPort.fetchDailyRates(monday) } returns emptyList()
        every { exchangeRateQueryPort.fetchDailyRates(friday) } returns expected

        // when
        val result = service.getDailyRates(monday)

        // then
        assertEquals(expected, result)
    }

    @Test
    fun `이전_영업일에도_없으면_계속_과거로_조회한다`() {
        // given
        val monday = LocalDate.of(2026, 5, 11)
        val friday = LocalDate.of(2026, 5, 8)
        val thursday = LocalDate.of(2026, 5, 7)
        val expected =
            listOf(
                환율(curUnit = "CNH", dealBasR = "216.83"),
            )
        every { exchangeRateQueryPort.fetchDailyRates(monday) } returns emptyList()
        every { exchangeRateQueryPort.fetchDailyRates(friday) } returns emptyList()
        every { exchangeRateQueryPort.fetchDailyRates(thursday) } returns expected

        // when
        val result = service.getDailyRates(monday)

        // then
        assertEquals(expected, result)
    }

    private fun 환율(
        curUnit: String,
        dealBasR: String,
    ): ExchangeRateQuote =
        ExchangeRateQuote(
            curUnit = curUnit,
            curNm = curUnit,
            ttb = dealBasR,
            tts = dealBasR,
            dealBasR = dealBasR,
            bkpr = dealBasR,
        )
}
