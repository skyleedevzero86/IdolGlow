package com.sleekydz86.idolglow.exchange.application

import com.sleekydz86.idolglow.exchange.adapter.web.dto.ExchangeBranchResponse
import com.sleekydz86.idolglow.exchange.domain.ExchangeBranch
import com.sleekydz86.idolglow.exchange.infrastructure.ExchangeBranchJpaRepository
import com.sleekydz86.idolglow.exchange.infrastructure.NaverDirectionsClient
import com.sleekydz86.idolglow.global.infrastructure.config.ExchangeAirportHubProperties
import org.springframework.stereotype.Service
import kotlin.math.abs

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
            val minutes = resolveDurationMinutes(row, hubLng, hubLat)
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

    private fun resolveDurationMinutes(row: ExchangeBranch, hubLng: Double, hubLat: Double): Int? {
        if (row.airportHub) {
            return 0
        }
        return naverDirectionsClient.drivingDurationMinutes(
            startLng = hubLng,
            startLat = hubLat,
            goalLng = row.lng,
            goalLat = row.lat,
        ) ?: demoDurationMinutes(row)
    }

    private fun demoDurationMinutes(row: ExchangeBranch): Int? =
        when {
            isSamePoint(row.lat, row.lng, 37.5635, 126.9826) -> 90
            isSamePoint(row.lat, row.lng, 37.5640, 126.9815) -> 83
            isSamePoint(row.lat, row.lng, 37.5632, 126.9850) -> 83
            isSamePoint(row.lat, row.lng, 37.5628, 126.9838) -> 83
            else -> null
        }

    private fun isSamePoint(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Boolean =
        abs(lat1 - lat2) < DEMO_POINT_TOLERANCE && abs(lng1 - lng2) < DEMO_POINT_TOLERANCE

    companion object {
        private const val DEMO_POINT_TOLERANCE = 0.0001
    }
}
