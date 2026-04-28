package com.sleekydz86.idolglow.subway.application.port.out

import com.sleekydz86.idolglow.subway.domain.SubwayExternalSearchHit

fun interface SubwayExternalStationSearchPort {
    fun searchByStationName(stationName: String): List<SubwayExternalSearchHit>
}
