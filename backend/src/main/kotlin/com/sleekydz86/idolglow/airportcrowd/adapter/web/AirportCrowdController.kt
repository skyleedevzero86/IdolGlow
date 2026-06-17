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
