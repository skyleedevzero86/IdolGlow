package com.sleekydz86.idolglow.subway.application.port.out

import com.sleekydz86.idolglow.subway.domain.SubwayStationLocation

fun interface SubwayStationLocationPort {
    fun locate(stationName: String): SubwayStationLocation?
}
