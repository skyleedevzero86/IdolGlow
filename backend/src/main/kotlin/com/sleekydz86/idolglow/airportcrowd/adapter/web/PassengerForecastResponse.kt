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
