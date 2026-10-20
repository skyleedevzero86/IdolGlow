package com.sleekydz86.idolglow.airportcrowd.adapter.web

import com.sleekydz86.idolglow.airportcrowd.application.ArrivalCongestionView
import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCrowdLevel
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes

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
                scheduleTime = IdolGlowDateTimes.toDisplay(view.scheduleTime),
                estimatedTime = IdolGlowDateTimes.toDisplay(view.estimatedTime),
                level = view.level.name.lowercase(),
                levelLabel =
                    when (view.level) {
                        DepartureCrowdLevel.SMOOTH -> "원활"
                        DepartureCrowdLevel.MODERATE -> "보통"
                        DepartureCrowdLevel.BUSY -> "혼잡"
                        DepartureCrowdLevel.HEAVY -> "매우혼잡"
                        DepartureCrowdLevel.UNKNOWN -> "확인불가"
                    },
            )
    }
}
