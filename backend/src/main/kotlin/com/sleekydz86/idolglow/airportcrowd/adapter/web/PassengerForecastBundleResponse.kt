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
