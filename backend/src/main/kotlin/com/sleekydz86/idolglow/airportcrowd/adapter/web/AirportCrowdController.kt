package com.sleekydz86.idolglow.airportcrowd.adapter.web

import com.sleekydz86.idolglow.airportcrowd.application.AirportCrowdQueryService
import com.sleekydz86.idolglow.airportcrowd.application.ArrivalCongestionView
import com.sleekydz86.idolglow.airportcrowd.application.CrowdCriteriaView
import com.sleekydz86.idolglow.airportcrowd.application.DepartureCongestionView
import com.sleekydz86.idolglow.airportcrowd.application.ParkingCongestionView
import com.sleekydz86.idolglow.airportcrowd.application.PassengerForecastBundleView
import com.sleekydz86.idolglow.airportcrowd.application.PassengerForecastView
import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCrowdLevel
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.format.DateTimeFormatter

@Tag(name = "Glow 공항인파")
@RestController
@RequestMapping("/mypage/airport-crowd", "/api/airport-crowd")
class AirportCrowdController(
    private val airportCrowdQueryService: AirportCrowdQueryService,
) {
    @GetMapping("/departure-congestion")
    fun departureCongestion(
        @LoginUser userId: Long,
        @RequestParam(required = false) terminalId: String?,
        @RequestParam(required = false) gateId: String?,
    ): List<DepartureCongestionResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return airportCrowdQueryService.listDepartureCongestion(terminalId = terminalId, gateId = gateId)
            .map { DepartureCongestionResponse.from(it) }
    }

    @GetMapping("/passenger-forecast")
    fun passengerForecast(
        @LoginUser userId: Long,
        @RequestParam(name = "selectdate", required = false) selectDate: Int?,
    ): PassengerForecastBundleResponse {
        check(userId > 0L) { "로그인이 필요합니다." }
        return PassengerForecastBundleResponse.from(airportCrowdQueryService.passengerForecast(selectDate))
    }

    @GetMapping("/arrivals-congestion")
    fun arrivalsCongestion(
        @LoginUser userId: Long,
        @RequestParam(required = false) terno: String?,
        @RequestParam(required = false) airport: String?,
    ): List<ArrivalCongestionResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return airportCrowdQueryService
            .listArrivalsCongestion(terminal = terno, airport = airport)
            .map { ArrivalCongestionResponse.from(it) }
    }

    @GetMapping("/parking-congestion")
    fun parkingCongestion(
        @LoginUser userId: Long,
        @RequestParam(required = false) terno: String?,
    ): List<ParkingCongestionResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return airportCrowdQueryService
            .listParkingCongestion(terminal = terno)
            .map { ParkingCongestionResponse.from(it) }
    }

    @GetMapping("/criteria")
    fun criteria(
        @LoginUser userId: Long,
        @RequestParam(required = false) zone: String?,
    ): List<CrowdCriteriaResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return airportCrowdQueryService
            .crowdCriteria(zone)
            .map { CrowdCriteriaResponse.from(it) }
    }
}

data class CrowdCriteriaResponse(
    val level: String,
    val levelLabel: String,
    val title: String,
    val description: String,
    val color: String,
) {
    companion object {
        fun from(view: CrowdCriteriaView): CrowdCriteriaResponse =
            CrowdCriteriaResponse(
                level = view.level.name.lowercase(),
                levelLabel = when (view.level) {
                    DepartureCrowdLevel.SMOOTH -> "정상"
                    DepartureCrowdLevel.MODERATE -> "다소 붐빔"
                    DepartureCrowdLevel.BUSY -> "붐빔"
                    DepartureCrowdLevel.HEAVY -> "매우 혼잡"
                    DepartureCrowdLevel.UNKNOWN -> "확인중"
                },
                title = view.title,
                description = view.description,
                color = view.color,
            )
    }
}

data class DepartureCongestionResponse(
    val gateId: String,
    val terminalId: String,
    val waitTimeMinutes: Int?,
    val waitLength: Int?,
    val occurredAt: String?,
    val operatingTime: String?,
    val level: String,
    val levelLabel: String,
) {
    companion object {
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun from(view: DepartureCongestionView): DepartureCongestionResponse =
            DepartureCongestionResponse(
                gateId = view.gateId,
                terminalId = view.terminalId,
                waitTimeMinutes = view.waitTimeMinutes,
                waitLength = view.waitLength,
                occurredAt = view.occurredAt?.format(FORMATTER),
                operatingTime = view.operatingTime,
                level = view.level.name.lowercase(),
                levelLabel = toLabel(view.level),
            )

        private fun toLabel(level: DepartureCrowdLevel): String =
            when (level) {
                DepartureCrowdLevel.SMOOTH -> "원활"
                DepartureCrowdLevel.MODERATE -> "보통"
                DepartureCrowdLevel.BUSY -> "혼잡"
                DepartureCrowdLevel.HEAVY -> "매우혼잡"
                DepartureCrowdLevel.UNKNOWN -> "확인불가"
            }
    }
}

data class ArrivalCongestionResponse(
    val terminal: String,
    val airport: String?,
    val entryGate: String?,
    val gateNumber: String?,
    val flightId: String?,
    val korean: Int?,
    val foreigner: Int?,
    val totalFlow: Int,
    val scheduleTime: String?,
    val estimatedTime: String?,
    val level: String,
    val levelLabel: String,
) {
    companion object {
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun from(view: ArrivalCongestionView): ArrivalCongestionResponse =
            ArrivalCongestionResponse(
                terminal = view.terminal,
                airport = view.airport,
                entryGate = view.entryGate,
                gateNumber = view.gateNumber,
                flightId = view.flightId,
                korean = view.korean,
                foreigner = view.foreigner,
                totalFlow = view.totalFlow,
                scheduleTime = view.scheduleTime?.format(FORMATTER),
                estimatedTime = view.estimatedTime?.format(FORMATTER),
                level = view.level.name.lowercase(),
                levelLabel = when (view.level) {
                    DepartureCrowdLevel.SMOOTH -> "원활"
                    DepartureCrowdLevel.MODERATE -> "보통"
                    DepartureCrowdLevel.BUSY -> "혼잡"
                    DepartureCrowdLevel.HEAVY -> "매우혼잡"
                    DepartureCrowdLevel.UNKNOWN -> "확인불가"
                },
            )
    }
}

data class PassengerForecastBundleResponse(
    val today: List<PassengerForecastResponse>,
    val tomorrow: List<PassengerForecastResponse>,
) {
    companion object {
        fun from(view: PassengerForecastBundleView): PassengerForecastBundleResponse =
            PassengerForecastBundleResponse(
                today = view.today.map(PassengerForecastResponse::from),
                tomorrow = view.tomorrow.map(PassengerForecastResponse::from),
            )
    }
}

data class PassengerForecastResponse(
    val date: String?,
    val timeSlot: String,
    val terminal1DepartureTotal: Int?,
    val terminal2DepartureTotal: Int?,
    val terminal1ArrivalTotal: Int?,
    val terminal2ArrivalTotal: Int?,
    val totalDeparture: Int,
    val level: String,
    val levelLabel: String,
) {
    companion object {
        fun from(view: PassengerForecastView): PassengerForecastResponse =
            PassengerForecastResponse(
                date = view.date,
                timeSlot = view.timeSlot,
                terminal1DepartureTotal = view.terminal1DepartureTotal,
                terminal2DepartureTotal = view.terminal2DepartureTotal,
                terminal1ArrivalTotal = view.terminal1ArrivalTotal,
                terminal2ArrivalTotal = view.terminal2ArrivalTotal,
                totalDeparture = view.totalDeparture,
                level = view.level.name.lowercase(),
                levelLabel = when (view.level) {
                    DepartureCrowdLevel.SMOOTH -> "원활"
                    DepartureCrowdLevel.MODERATE -> "보통"
                    DepartureCrowdLevel.BUSY -> "혼잡"
                    DepartureCrowdLevel.HEAVY -> "매우혼잡"
                    DepartureCrowdLevel.UNKNOWN -> "확인불가"
                },
            )
    }
}

data class ParkingCongestionResponse(
    val terminal: String?,
    val floor: String,
    val parking: Int?,
    val parkingArea: Int?,
    val available: Int?,
    val occupancyRate: Double?,
    val observedAt: String?,
    val level: String,
    val levelLabel: String,
) {
    companion object {
        private val FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

        fun from(view: ParkingCongestionView): ParkingCongestionResponse =
            ParkingCongestionResponse(
                terminal = view.terminal,
                floor = view.floor,
                parking = view.parking,
                parkingArea = view.parkingArea,
                available = view.available,
                occupancyRate = view.occupancyRate?.let { kotlin.math.round(it * 10.0) / 10.0 },
                observedAt = view.observedAt?.format(FORMATTER),
                level = view.level.name.lowercase(),
                levelLabel = when (view.level) {
                    DepartureCrowdLevel.SMOOTH -> "원활"
                    DepartureCrowdLevel.MODERATE -> "보통"
                    DepartureCrowdLevel.BUSY -> "혼잡"
                    DepartureCrowdLevel.HEAVY -> "매우혼잡"
                    DepartureCrowdLevel.UNKNOWN -> "확인불가"
                },
            )
    }
}
