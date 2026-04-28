package com.sleekydz86.idolglow.subway.application.port.out

import com.sleekydz86.idolglow.subway.domain.SubwayStationStop

fun interface SubwayStationOrderPort {
    fun orderedStops(lineId: String): List<SubwayStationStop>
}
