package com.sleekydz86.idolglow.airportcrowd.adapter.web

import com.sleekydz86.idolglow.airportcrowd.application.ParkingCongestionView
import com.sleekydz86.idolglow.airportcrowd.domain.DepartureCrowdLevel
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes

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
        fun from(view: ParkingCongestionView): ParkingCongestionResponse =
            ParkingCongestionResponse(
                terminal = view.terminal,
                floor = view.floor,
                parking = view.parking,
                parkingArea = view.parkingArea,
                available = view.available,
                occupancyRate = view.occupancyRate?.let { kotlin.math.round(it * 10.0) / 10.0 },
                observedAt = IdolGlowDateTimes.toDisplay(view.observedAt),
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
