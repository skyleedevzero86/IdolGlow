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
