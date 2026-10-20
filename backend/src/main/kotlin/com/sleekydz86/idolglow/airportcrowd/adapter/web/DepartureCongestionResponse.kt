package com.sleekydz86.idolglow.airportcrowd.adapter.web

import com.sleekydz86.idolglow.airportcrowd.application.DepartureCongestionView
import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCrowdLevel
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes

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
        fun from(view: DepartureCongestionView): DepartureCongestionResponse =
            DepartureCongestionResponse(
                gateId = view.gateId,
                terminalId = view.terminalId,
                waitTimeMinutes = view.waitTimeMinutes,
                waitLength = view.waitLength,
                occurredAt = IdolGlowDateTimes.toDisplay(view.occurredAt),
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
