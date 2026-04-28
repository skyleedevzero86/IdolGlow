package com.sleekydz86.idolglow.exchange.application

import com.sleekydz86.idolglow.exchange.domain.ExchangeBranch
import com.sleekydz86.idolglow.exchange.infrastructure.ExchangeBranchJpaRepository
import com.sleekydz86.idolglow.exchange.infrastructure.NaverDirectionsClient
import com.sleekydz86.idolglow.exchangerate.application.port.out.ExchangeRateQueryPort
import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import com.sleekydz86.idolglow.global.infrastructure.config.ExchangeAirportHubProperties
import io.mockk.every
import io.mockk.firstArg
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.LocalDate

class ExchangeBranchQueryServiceTest {

    private val exchangeBranchJpaRepository = mockk<ExchangeBranchJpaRepository>()
    private val exchangeRateQueryPort = mockk<ExchangeRateQueryPort>()
    private val naverDirectionsClient = mockk<NaverDirectionsClient>()
    private val exchangeAirportHubProperties = ExchangeAirportHubProperties(
        longitude = 126.4407,
        latitude = 37.4602,
    )

    private val service = ExchangeBranchQueryService(
        exchangeBranchJpaRepository = exchangeBranchJpaRepository,
        exchangeRateQueryPort = exchangeRateQueryPort,
        naverDirectionsClient = naverDirectionsClient,
        exchangeAirportHubProperties = exchangeAirportHubProperties,
    )

    @Test
    fun `falls back to demo travel minutes when directions api is unavailable`() {
        val jpyRows = jpyTemplateRows()
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("JPY") } returns jpyRows
        every { naverDirectionsClient.drivingDurationMinutes(any(), any(), any(), any()) } returns null

        val result = service.listBranchesWithDrivingMinutes("JPY")

        assertEquals(listOf(0, 90, 83, 83, 83), result.map { it.durationMinutesFromAirport })
    }

    @Test
    fun `supplements sparse currency rows using branch template`() {
        val jpyRows = jpyTemplateRows()
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("JPY") } returns jpyRows
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("CNY") } returns listOf(
            branch(
                id = 11L,
                name = "airport",
                lat = 37.4602,
                lng = 126.4407,
                rate = "188.4",
                currency = "CNY",
                sortOrder = 0,
                airportHub = true,
            ),
        )
        every { naverDirectionsClient.drivingDurationMinutes(any(), any(), any(), any()) } returns null

        val result = service.listBranchesWithDrivingMinutes("CNY")

        assertEquals(5, result.size)
        assertEquals(listOf("CNY", "CNY", "CNY", "CNY", "CNY"), result.map { it.currency })
        assertEquals(listOf(0, 90, 83, 83, 83), result.map { it.durationMinutesFromAirport })
    }

    @Test
    fun `creates full branch list from official rate when db rows are missing`() {
        val jpyRows = jpyTemplateRows()
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("JPY") } returns jpyRows
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("HKD") } returns emptyList()
        every { exchangeRateQueryPort.fetchDailyRates(null) } returns emptyList()
        every { exchangeRateQueryPort.fetchDailyRates(any<LocalDate>()) } answers {
            val date = firstArg<LocalDate>()
            if (date == LocalDate.now().minusDays(1)) {
                listOf(
                    ExchangeRateQuote(
                        curUnit = "HKD",
                        curNm = "Hong Kong Dollar",
                        ttb = "188.00",
                        tts = "190.00",
                        dealBasR = "189.03",
                        bkpr = "189",
                    ),
                )
            } else {
                emptyList()
            }
        }
        every { naverDirectionsClient.drivingDurationMinutes(any(), any(), any(), any()) } returns null

        val result = service.listBranchesWithDrivingMinutes("HKD")

        assertEquals(5, result.size)
        assertEquals(listOf("HKD", "HKD", "HKD", "HKD", "HKD"), result.map { it.currency })
        assertEquals(BigDecimal("189.030000"), result.first().rate)
    }

    @Test
    fun `uses directions api result when available`() {
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("JPY") } returns listOf(
            branch(id = 2L, name = "branch", lat = 37.5635, lng = 126.9826, sortOrder = 1),
        )
        every {
            naverDirectionsClient.drivingDurationMinutes(
                startLng = 126.4407,
                startLat = 37.4602,
                goalLng = 126.9826,
                goalLat = 37.5635,
            )
        } returns 77

        val result = service.listBranchesWithDrivingMinutes("JPY")

        assertEquals(listOf(77), result.map { it.durationMinutesFromAirport })
    }

    private fun jpyTemplateRows(): List<ExchangeBranch> = listOf(
        branch(id = 1L, name = "airport", lat = 37.4602, lng = 126.4407, rate = "9.33", sortOrder = 0, airportHub = true),
        branch(id = 2L, name = "myeongdong-a", lat = 37.5635, lng = 126.9826, rate = "9.23", sortOrder = 1),
        branch(id = 3L, name = "myeongdong-b", lat = 37.5640, lng = 126.9815, rate = "9.23", sortOrder = 2),
        branch(id = 4L, name = "myeongdong-c", lat = 37.5632, lng = 126.9850, rate = "9.23", sortOrder = 3),
        branch(id = 5L, name = "myeongdong-d", lat = 37.5628, lng = 126.9838, rate = "9.2283", sortOrder = 4),
    )

    private fun branch(
        id: Long,
        name: String,
        lat: Double,
        lng: Double,
        rate: String = "9.23",
        currency: String = "JPY",
        sortOrder: Int,
        airportHub: Boolean = false,
    ): ExchangeBranch = ExchangeBranch(
        id = id,
        name = name,
        rate = BigDecimal(rate),
        currency = currency,
        lat = lat,
        lng = lng,
        sortOrder = sortOrder,
        airportHub = airportHub,
    )
}
