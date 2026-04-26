package com.sleekydz86.idolglow.exchange.application

import com.sleekydz86.idolglow.exchange.adapter.web.dto.ExchangeBranchResponse
import com.sleekydz86.idolglow.exchange.infrastructure.ExchangeBranchJpaRepository
import com.sleekydz86.idolglow.exchange.infrastructure.NaverDirectionsClient
import com.sleekydz86.idolglow.global.infrastructure.config.ExchangeAirportHubProperties
import org.springframework.stereotype.Service

@Service
class ExchangeBranchQueryService(
    private val exchangeBranchJpaRepository: ExchangeBranchJpaRepository,
    private val naverDirectionsClient: NaverDirectionsClient,
    private val exchangeAirportHubProperties: ExchangeAirportHubProperties,
) {

    fun listBranchesWithDrivingMinutes(currencyParam: String): List<ExchangeBranchResponse> {
        val currency = currencyParam.trim().uppercase().substringBefore('(').trim()
        val rows = exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc(currency)
        val hubLng = exchangeAirportHubProperties.longitude
        val hubLat = exchangeAirportHubProperties.latitude
        return rows.map { row ->
            val minutes: Int? = if (row.airportHub) {
                0
            } else {
                naverDirectionsClient.drivingDurationMinutes(
                    startLng = hubLng,
                    startLat = hubLat,
                    goalLng = row.lng,
                    goalLat = row.lat,
                )
            }
            ExchangeBranchResponse(
                branchId = row.id,
                name = row.name,
                rate = row.rate,
                currency = row.currency,
                lat = row.lat,
                lng = row.lng,
                airportHub = row.airportHub,
                durationMinutesFromAirport = minutes,
            )
        }
    }
}
