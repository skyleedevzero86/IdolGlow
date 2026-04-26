package com.sleekydz86.idolglow.exchange.application

import com.sleekydz86.idolglow.exchange.domain.ExchangeBranch
import com.sleekydz86.idolglow.exchange.infrastructure.ExchangeBranchJpaRepository
import com.sleekydz86.idolglow.exchange.infrastructure.NaverDirectionsClient
import com.sleekydz86.idolglow.global.infrastructure.config.ExchangeAirportHubProperties
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ExchangeBranchQueryServiceTest {

    private val exchangeBranchJpaRepository = mockk<ExchangeBranchJpaRepository>()
    private val naverDirectionsClient = mockk<NaverDirectionsClient>()
    private val exchangeAirportHubProperties = ExchangeAirportHubProperties(
        longitude = 126.4407,
        latitude = 37.4602,
    )

    private val service = ExchangeBranchQueryService(
        exchangeBranchJpaRepository = exchangeBranchJpaRepository,
        naverDirectionsClient = naverDirectionsClient,
        exchangeAirportHubProperties = exchangeAirportHubProperties,
    )

    @Test
    fun `falls back to demo travel minutes when directions api is unavailable`() {
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("JPY") } returns listOf(
            branch(id = 1L, name = "IdolGlow 공항 환전", lat = 37.4602, lng = 126.4407, airportHub = true),
            branch(id = 2L, name = "명동 대사관", lat = 37.5635, lng = 126.9826),
            branch(id = 3L, name = "명동 제1교류센터", lat = 37.5640, lng = 126.9815),
            branch(id = 4L, name = "환전 카페 (구 입핀상)", lat = 37.5632, lng = 126.9850),
            branch(id = 5L, name = "명동 돈 상자", lat = 37.5628, lng = 126.9838),
        )
        every { naverDirectionsClient.drivingDurationMinutes(any(), any(), any(), any()) } returns null

        val result = service.listBranchesWithDrivingMinutes("JPY")

        assertEquals(listOf(0, 90, 83, 83, 83), result.map { it.durationMinutesFromAirport })
    }

    @Test
    fun `uses directions api result when available`() {
        every { exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc("JPY") } returns listOf(
            branch(id = 2L, name = "명동 대사관", lat = 37.5635, lng = 126.9826),
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

    private fun branch(
        id: Long,
        name: String,
        lat: Double,
        lng: Double,
        airportHub: Boolean = false,
    ): ExchangeBranch = ExchangeBranch(
        id = id,
        name = name,
        rate = BigDecimal("9.23"),
        currency = "JPY",
        lat = lat,
        lng = lng,
        sortOrder = id.toInt(),
        airportHub = airportHub,
    )
}
