package com.sleekydz86.idolglow.subway.application.port.incoming

import com.sleekydz86.idolglow.subway.domain.SubwayLine
import com.sleekydz86.idolglow.subway.domain.SubwayPageEnrichment
import com.sleekydz86.idolglow.subway.domain.SubwayStationStop

data class SubwayStationPageResult(
    val line: SubwayLine,
    val station: SubwayStationStop,
    val prevStation: SubwayStationStop,
    val nextStation: SubwayStationStop,
    val enrichment: SubwayPageEnrichment,
)
