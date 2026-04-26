package com.sleekydz86.idolglow.exchangerate.application

import com.sleekydz86.idolglow.exchange.domain.ExchangeBranch
import com.sleekydz86.idolglow.exchange.infrastructure.ExchangeBranchJpaRepository
import com.sleekydz86.idolglow.exchangerate.application.port.out.ExchangeRateQueryPort
import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ExchangeRateQueryServiceTest {

    private val exchangeRateQueryPort = mockk<ExchangeRateQueryPort>()
    private val exchangeBranchJpaRepository = mockk<ExchangeBranchJpaRepository>()

    private val service = ExchangeRateQueryService(
        exchangeRateQueryPort = exchangeRateQueryPort,
        exchangeBranchJpaRepository = exchangeBranchJpaRepository,
    )

    @Test
    fun `returns official rates when exchange api responds`() {
        val official = listOf(
            ExchangeRateQuote(
                curUnit = "JPY(100)",
                curNm = "엔",
                ttb = "920.00",
                tts = "946.00",
                dealBasR = "933.00",
                bkpr = "933",
            ),
        )
        every { exchangeRateQueryPort.fetchDailyRates(null) } returns official

        val result = service.getDailyRates(null)

        assertEquals(official, result)
    }

    @Test
    fun `returns fallback branch rates when exchange api is empty`() {
        every { exchangeRateQueryPort.fetchDailyRates(null) } returns emptyList()
        every { exchangeBranchJpaRepository.findAll() } returns listOf(
            branch(id = 1L, currency = "JPY", rate = "9.33", airportHub = true, sortOrder = 0),
            branch(id = 2L, currency = "JPY", rate = "9.23", airportHub = false, sortOrder = 1),
            branch(id = 3L, currency = "CNY", rate = "192.0", airportHub = true, sortOrder = 0),
            branch(id = 4L, currency = "USD", rate = "1370.0", airportHub = true, sortOrder = 0),
            branch(id = 5L, currency = "EUR", rate = "1480.0", airportHub = true, sortOrder = 0),
        )

        val result = service.getDailyRates(null)

        assertEquals(listOf("CNY", "EUR", "JPY", "USD"), result.map { it.curUnit })
        assertEquals(listOf("192", "1480", "9.33", "1370"), result.map { it.dealBasR })
    }

    private fun branch(
        id: Long,
        currency: String,
        rate: String,
        airportHub: Boolean,
        sortOrder: Int,
    ): ExchangeBranch = ExchangeBranch(
        id = id,
        name = "${currency}-branch",
        rate = BigDecimal(rate),
        currency = currency,
        lat = 37.0,
        lng = 127.0,
        sortOrder = sortOrder,
        airportHub = airportHub,
    )
}
