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
