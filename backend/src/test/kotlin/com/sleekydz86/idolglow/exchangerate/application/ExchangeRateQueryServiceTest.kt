package com.sleekydz86.idolglow.exchangerate.application

import com.sleekydz86.idolglow.exchangerate.application.port.out.ExchangeRateQueryPort
import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import java.time.LocalDate

@DisplayName("환율 조회")
class ExchangeRateQueryServiceTest {
    private val exchangeRateQueryPort = mockk<ExchangeRateQueryPort>()
    private val service = ExchangeRateQueryService(exchangeRateQueryPort)

    @DisplayName("조회일 데이터가 있으면 해당 일자 환율을 반환한다")
    @Test
    fun returnsRateForRequestedDate() {

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

    @DisplayName("조회일 데이터가 없으면 이전 영업일 환율을 조회한다")
    @Test
    fun fallsBackToPreviousBusinessDay() {

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

    @DisplayName("이전 영업일에도 없으면 계속 과거로 조회한다")
    @Test
    fun keepsLookingFurtherBack() {

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
