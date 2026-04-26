package com.sleekydz86.idolglow.subway.application.port.out

import com.sleekydz86.idolglow.subway.domain.SubwayPageEnrichment

fun interface SubwayPageEnrichmentPort {
    fun loadFor(lineId: String, stationCd: String, stationDisplayName: String): SubwayPageEnrichment
}
