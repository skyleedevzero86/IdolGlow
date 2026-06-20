package com.sleekydz86.idolglow.airportcrowd.adapter.web

import com.sleekydz86.idolglow.airportcrowd.application.AirportCrowdQueryService
import com.sleekydz86.idolglow.global.adapter.resolver.LoginUser
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@Tag(name = "Glow 공항인파", description = "인천공항 출·입국 및 주차 혼잡도·승객 예측 API")
@RestController
@RequestMapping("/mypage/airport-crowd", "/api/airport-crowd")
class AirportCrowdController(
    private val airportCrowdQueryService: AirportCrowdQueryService,
) {
    @Operation(summary = "출국장 혼잡도 조회")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/departure-congestion")
    fun departureCongestion(
        @LoginUser userId: Long,
        @RequestParam(required = false) terminalId: String?,
        @RequestParam(required = false) gateId: String?,
    ): List<DepartureCongestionResponse> {
        check(userId > 0L) { "로그인이 필요합니다." }
        return airportCrowdQueryService
            .listDepartureCongestion(terminalId = terminalId, gateId = gateId)
            .map { DepartureCongestionResponse.from(it) }
    }

    @Operation(summary = "승객 혼잡 예측 조회", description = "selectdate=0(오늘), 1(내일)")
    @SecurityRequirement(name = "bearerAuth")
    @GetMapping("/passenger-forecast")
    fun passengerForecast(
        @LoginUser userId: Long,
        @RequestParam(name = "selectdate", required = false) selectDate: Int?,
    ): PassengerForecastBundleResponse {
        check(userId > 0L) { "로그인이 필요합니다." }
        return PassengerForecastBundleResponse.from(airportCrowdQueryService.passengerForecast(selectDate))
    }

    @Operation(summary = "입국장 혼잡도 조회")
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(summary = "주차장 혼잡도 조회")
    @SecurityRequirement(name = "bearerAuth")
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

    @Operation(summary = "혼잡도 기준 조회")
    @SecurityRequirement(name = "bearerAuth")
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
