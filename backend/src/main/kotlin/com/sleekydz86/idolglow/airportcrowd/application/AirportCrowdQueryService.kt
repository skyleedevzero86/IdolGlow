package com.sleekydz86.idolglow.airportcrowd.application

import com.sleekydz86.idolglow.airportcrowd.application.port.out.ArrivalCongestionQueryPort
import com.sleekydz86.idolglow.airportcrowd.application.port.out.DepartureCongestionQueryPort
import com.sleekydz86.idolglow.airportcrowd.application.port.out.ParkingCongestionQueryPort
import com.sleekydz86.idolglow.airportcrowd.application.port.out.PassengerForecastQueryPort
import com.sleekydz86.idolglow.airportcrowd.domain.ArrivalCongestion
import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCongestion
import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCrowdLevel
import com.sleekydz86.idolglow.airportcrowd.domain.PassengerForecast
import com.sleekydz86.idolglow.airportcrowd.domain.ParkingCongestion
import org.springframework.stereotype.Service

@Service
class AirportCrowdQueryService(
    private val departureCongestionQueryPort: DepartureCongestionQueryPort,
    private val arrivalCongestionQueryPort: ArrivalCongestionQueryPort,
    private val passengerForecastQueryPort: PassengerForecastQueryPort,
    private val parkingCongestionQueryPort: ParkingCongestionQueryPort,
) {
    fun listDepartureCongestion(
        terminalId: String?,
        gateId: String?,
    ): List<DepartureCongestionView> {
        val normalizedTerminal = terminalId?.trim()?.uppercase()?.takeIf { it in SUPPORTED_TERMINAL_IDS }
        val normalizedGate = gateId?.trim()?.uppercase()?.takeIf { it in SUPPORTED_GATE_IDS }
        return departureCongestionQueryPort
            .fetchCurrent(
                terminalId = normalizedTerminal,
                gateId = normalizedGate,
                pageNo = 1,
                numOfRows = 1000,
            )
            .sortedWith(compareBy({ it.gateId }, { it.occurredAt }))
            .map {
                DepartureCongestionView(
                    gateId = it.gateId,
                    terminalId = it.terminalId,
                    waitTimeMinutes = it.waitTimeMinutes,
                    waitLength = it.waitLength,
                    occurredAt = it.occurredAt,
                    operatingTime = it.operatingTime,
                    level = toLevel(it.waitTimeMinutes),
                )
            }
    }

    fun crowdCriteria(zone: String?): List<CrowdCriteriaView> {
        val normalizedZone = zone?.trim()?.lowercase()
        return when (normalizedZone) {
            "arrival" -> listOf(
                crowdCriteria(DepartureCrowdLevel.SMOOTH, "정상", "시간당 1,000명 미만", "#0ac84c"),
                crowdCriteria(DepartureCrowdLevel.MODERATE, "다소 붐비네요", "시간당 1,000~2,500명", "#fdd83f"),
                crowdCriteria(DepartureCrowdLevel.BUSY, "붐비다", "시간당 2,500~4,000명", "#ff8a00"),
                crowdCriteria(DepartureCrowdLevel.HEAVY, "정말 붐비고 있어요", "시간당 4,000명 이상이 유입됩니다", "#f24548"),
            )
            "parking" -> listOf(
                crowdCriteria(DepartureCrowdLevel.SMOOTH, "정상", "주차 점유율 55% 미만", "#0ac84c"),
                crowdCriteria(DepartureCrowdLevel.MODERATE, "다소 붐비네요", "주차 점유율 55~75%", "#fdd83f"),
                crowdCriteria(DepartureCrowdLevel.BUSY, "붐비다", "주차 점유율 75~90%", "#ff8a00"),
                crowdCriteria(DepartureCrowdLevel.HEAVY, "정말 붐비고 있어요", "주차 점유율 90% 이상", "#f24548"),
            )
            else -> listOf(
                crowdCriteria(DepartureCrowdLevel.SMOOTH, "정상", "대기시간 20분 미만", "#0ac84c"),
                crowdCriteria(DepartureCrowdLevel.MODERATE, "다소 붐비네요", "대기시간 20~30분", "#fdd83f"),
                crowdCriteria(DepartureCrowdLevel.BUSY, "붐비다", "대기시간 30~40분", "#ff8a00"),
                crowdCriteria(DepartureCrowdLevel.HEAVY, "정말 붐비고 있어요", "대기시간 40분 이상", "#f24548"),
            )
        }
    }

    private fun crowdCriteria(
        level: DepartureCrowdLevel,
        title: String,
        description: String,
        color: String,
    ): CrowdCriteriaView =
        CrowdCriteriaView(
            level = level,
            title = title,
            description = description,
            color = color,
        )

    private fun toLevel(waitTimeMinutes: Int?): DepartureCrowdLevel {
        if (waitTimeMinutes == null) return DepartureCrowdLevel.UNKNOWN
        return when {
            waitTimeMinutes < 20 -> DepartureCrowdLevel.SMOOTH
            waitTimeMinutes < 30 -> DepartureCrowdLevel.MODERATE
            waitTimeMinutes < 40 -> DepartureCrowdLevel.BUSY
            waitTimeMinutes >= 40 -> DepartureCrowdLevel.HEAVY
            else -> DepartureCrowdLevel.UNKNOWN
        }
    }

    companion object {
        private val SUPPORTED_TERMINAL_IDS = setOf("P01", "P03")
        private val SUPPORTED_ARRIVAL_TERMINALS = setOf("T1", "T2")
        private val AIRPORT_CODE_REGEX = Regex("^[A-Z]{3}$")

        private val SUPPORTED_GATE_IDS = setOf(
            "DG1_W", "DG1_E",
            "DG2_W", "DG2_E",
            "DG3_W", "DG3_E",
            "DG4_W", "DG4_E",
            "DG5_W", "DG5_E",
            "DG6_W", "DG6_E",
        )
    }

    fun passengerForecast(selectDate: Int?): PassengerForecastBundleView {
        val dates = when (selectDate) {
            0 -> listOf(0)
            1 -> listOf(1)
            null -> listOf(0, 1)
            else -> throw IllegalArgumentException("selectdate는 0(오늘) 또는 1(내일)이어야 합니다.")
        }
        val today = if (0 in dates) passengerForecastQueryPort.fetch(0).sortedBy { it.timeSlot } else emptyList()
        val tomorrow = if (1 in dates) passengerForecastQueryPort.fetch(1).sortedBy { it.timeSlot } else emptyList()
        return PassengerForecastBundleView(
            today = today.map(::toForecastView),
            tomorrow = tomorrow.map(::toForecastView),
        )
    }

    private fun toForecastView(item: PassengerForecast): PassengerForecastView {
        val totalDeparture = (item.terminal1DepartureTotal ?: 0) + (item.terminal2DepartureTotal ?: 0)
        return PassengerForecastView(
            date = item.date?.toString(),
            timeSlot = item.timeSlot,
            terminal1DepartureTotal = item.terminal1DepartureTotal,
            terminal2DepartureTotal = item.terminal2DepartureTotal,
            terminal1ArrivalTotal = item.terminal1ArrivalTotal,
            terminal2ArrivalTotal = item.terminal2ArrivalTotal,
            totalDeparture = totalDeparture,
            level = toLevelByForecast(totalDeparture),
        )
    }

    private fun toLevelByForecast(totalDeparture: Int): DepartureCrowdLevel =
        when {
            totalDeparture < 1000 -> DepartureCrowdLevel.SMOOTH
            totalDeparture < 2500 -> DepartureCrowdLevel.MODERATE
            totalDeparture < 4000 -> DepartureCrowdLevel.BUSY
            else -> DepartureCrowdLevel.HEAVY
        }

    fun listArrivalsCongestion(
        terminal: String?,
        airport: String?,
    ): List<ArrivalCongestionView> {
        val normalizedTerminal = terminal?.trim()?.uppercase()?.takeIf { it in SUPPORTED_ARRIVAL_TERMINALS }
        val normalizedAirport = airport?.trim()?.uppercase()?.takeIf { it.matches(AIRPORT_CODE_REGEX) }
        return arrivalCongestionQueryPort
            .fetchCurrent(
                terminal = normalizedTerminal,
                airport = normalizedAirport,
                pageNo = 1,
                numOfRows = 1000,
            )
            .sortedWith(compareBy({ it.entryGate }, { it.scheduleTime }))
            .map(::toArrivalView)
    }

    private fun toArrivalView(item: ArrivalCongestion): ArrivalCongestionView {
        val totalFlow = (item.korean ?: 0) + (item.foreigner ?: 0)
        return ArrivalCongestionView(
            terminal = item.terminal,
            airport = item.airport,
            entryGate = item.entryGate,
            gateNumber = item.gateNumber,
            flightId = item.flightId,
            korean = item.korean,
            foreigner = item.foreigner,
            totalFlow = totalFlow,
            scheduleTime = item.scheduleTime,
            estimatedTime = item.estimatedTime,
            level = toLevelByForecast(totalFlow),
        )
    }

    fun listParkingCongestion(terminal: String?): List<ParkingCongestionView> {
        val normalizedTerminal = terminal?.trim()?.uppercase()?.takeIf { it in SUPPORTED_ARRIVAL_TERMINALS }
        return parkingCongestionQueryPort
            .fetchCurrent(pageNo = 1, numOfRows = 1000)
            .asSequence()
            .filter { normalizedTerminal == null || it.terminal == normalizedTerminal }
            .sortedWith(compareBy({ it.terminal }, { it.floor }))
            .map(::toParkingView)
            .toList()
    }

    private fun toParkingView(item: ParkingCongestion): ParkingCongestionView {
        val parking = item.parking
        val parkingArea = item.parkingArea?.takeIf { it > 0 }
        val occupancyRate = if (parking != null && parkingArea != null) (parking * 100.0) / parkingArea else null
        val available = if (parking != null && parkingArea != null) (parkingArea - parking).coerceAtLeast(0) else null
        val level = when {
            occupancyRate == null -> DepartureCrowdLevel.UNKNOWN
            occupancyRate < 55.0 -> DepartureCrowdLevel.SMOOTH
            occupancyRate < 75.0 -> DepartureCrowdLevel.MODERATE
            occupancyRate < 90.0 -> DepartureCrowdLevel.BUSY
            else -> DepartureCrowdLevel.HEAVY
        }
        return ParkingCongestionView(
            terminal = item.terminal,
            floor = item.floor,
            parking = parking,
            parkingArea = item.parkingArea,
            available = available,
            occupancyRate = occupancyRate,
            observedAt = item.observedAt,
            level = level,
        )
    }

}

data class DepartureCongestionView(
    val gateId: String,
    val terminalId: String,
    val waitTimeMinutes: Int?,
    val waitLength: Int?,
    val occurredAt: java.time.LocalDateTime?,
    val operatingTime: String?,
    val level: DepartureCrowdLevel,
)

data class CrowdCriteriaView(
    val level: DepartureCrowdLevel,
    val title: String,
    val description: String,
    val color: String,
)

data class PassengerForecastBundleView(
    val today: List<PassengerForecastView>,
    val tomorrow: List<PassengerForecastView>,
)

data class PassengerForecastView(
    val date: String?,
    val timeSlot: String,
    val terminal1DepartureTotal: Int?,
    val terminal2DepartureTotal: Int?,
    val terminal1ArrivalTotal: Int?,
    val terminal2ArrivalTotal: Int?,
    val totalDeparture: Int,
    val level: DepartureCrowdLevel,
)

data class ArrivalCongestionView(
    val terminal: String,
    val airport: String?,
    val entryGate: String?,
    val gateNumber: String?,
    val flightId: String?,
    val korean: Int?,
    val foreigner: Int?,
    val totalFlow: Int,
    val scheduleTime: java.time.LocalDateTime?,
    val estimatedTime: java.time.LocalDateTime?,
    val level: DepartureCrowdLevel,
)

data class ParkingCongestionView(
    val terminal: String?,
    val floor: String,
    val parking: Int?,
    val parkingArea: Int?,
    val available: Int?,
    val occupancyRate: Double?,
    val observedAt: java.time.LocalDateTime?,
    val level: DepartureCrowdLevel,
)
