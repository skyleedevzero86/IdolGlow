package com.sleekydz86.idolglow.subway.application.port.out

import com.sleekydz86.idolglow.subway.domain.SubwayNearbyPlace
import com.sleekydz86.idolglow.subway.domain.SubwayStationLocation

fun interface SubwayNearbyPlacePort {
    fun nearby(location: SubwayStationLocation): List<SubwayNearbyPlace>
}
