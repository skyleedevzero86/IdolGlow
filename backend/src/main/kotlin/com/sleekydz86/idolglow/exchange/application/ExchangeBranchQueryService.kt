package com.sleekydz86.idolglow.exchange.application

import com.sleekydz86.idolglow.exchange.adapter.web.dto.ExchangeBranchResponse
import com.sleekydz86.idolglow.exchange.domain.ExchangeBranch
import com.sleekydz86.idolglow.exchange.infrastructure.ExchangeBranchJpaRepository
import com.sleekydz86.idolglow.exchangerate.application.port.out.ExchangeRateQueryPort
import com.sleekydz86.idolglow.exchangerate.domain.ExchangeRateQuote
import com.sleekydz86.idolglow.exchange.infrastructure.NaverDirectionsClient
import com.sleekydz86.idolglow.global.infrastructure.config.ExchangeAirportHubProperties
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.LocalDate
import kotlin.math.abs

@Service
class ExchangeBranchQueryService(
    private val exchangeBranchJpaRepository: ExchangeBranchJpaRepository,
    private val exchangeRateQueryPort: ExchangeRateQueryPort,
    private val naverDirectionsClient: NaverDirectionsClient,
    private val exchangeAirportHubProperties: ExchangeAirportHubProperties,
) {

    fun listBranchesWithDrivingMinutes(currencyParam: String): List<ExchangeBranchResponse> {
        val currency = currencyParam.trim().uppercase().substringBefore('(').trim()
        val rows = loadBranchRows(currency)
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

    private fun loadBranchRows(currency: String): List<ExchangeBranch> {
        val rows = exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc(currency)
        if (rows.size >= MIN_BRANCH_COUNT) {
            return rows
        }
        val templates = exchangeBranchJpaRepository.findByCurrencyOrderBySortOrderAsc(TEMPLATE_CURRENCY)
        if (templates.isEmpty()) {
            return rows
        }

        val selectedBySort = rows.associateBy { it.sortOrder }
        val targetHubRate = rows.firstOrNull { it.airportHub }?.rate ?: officialHubRate(currency) ?: return rows
        val templateHubRate = templates.firstOrNull { it.airportHub }?.rate ?: templates.first().rate

        return templates.map { template ->
            selectedBySort[template.sortOrder]
                ?: synthesizeBranch(
                    template = template,
                    currency = currency,
                    targetHubRate = targetHubRate,
                    templateHubRate = templateHubRate,
                )
        }
    }

    private fun synthesizeBranch(
        template: ExchangeBranch,
        currency: String,
        targetHubRate: BigDecimal,
        templateHubRate: BigDecimal,
    ): ExchangeBranch {
        val scaledRate = if (template.airportHub) {
            targetHubRate
        } else {
            targetHubRate
                .multiply(template.rate)
                .divide(templateHubRate, 4, RoundingMode.HALF_UP)
        }
        return ExchangeBranch(
            id = -1_000L - template.sortOrder.toLong(),
            name = template.name,
            rate = scaledRate,
            currency = currency,
            lat = template.lat,
            lng = template.lng,
            sortOrder = template.sortOrder,
            airportHub = template.airportHub,
        )
    }

    private fun officialHubRate(currency: String): BigDecimal? {
        for (offset in 0..OFFICIAL_LOOKBACK_DAYS) {
            val date = if (offset == 0) null else LocalDate.now().minusDays(offset.toLong())
            val rates = exchangeRateQueryPort.fetchDailyRates(date)
            val match = rates.firstOrNull { currencyBaseCode(it.curUnit) == currency } ?: continue
            val perUnit = krwPerOneUnit(match) ?: continue
            return perUnit
        }
        return null
    }

    private fun currencyBaseCode(code: String): String {
        val compact = code.trim().uppercase().replace(" ", "")
        val head = Regex("^([A-Z]{3})(?:\\(\\d+\\))?$").find(compact)?.groupValues?.get(1) ?: compact
        return when (head) {
            "CNH" -> "CNY"
            else -> head
        }
    }

    private fun krwPerOneUnit(rate: ExchangeRateQuote): BigDecimal? {
        val deal = rate.dealBasR.replace(",", "").trim().takeIf { it.isNotEmpty() }?.toBigDecimalOrNull() ?: return null
        val divisor = Regex("\\((\\d+)\\)\\s*$").find(rate.curUnit)?.groupValues?.get(1)?.toBigDecimalOrNull()
            ?: BigDecimal.ONE
        if (divisor.compareTo(BigDecimal.ZERO) <= 0) return null
        return deal.divide(divisor, 6, RoundingMode.HALF_UP)
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
        private const val MIN_BRANCH_COUNT = 5
        private const val OFFICIAL_LOOKBACK_DAYS = 7
        private const val TEMPLATE_CURRENCY = "JPY"
    }
}
