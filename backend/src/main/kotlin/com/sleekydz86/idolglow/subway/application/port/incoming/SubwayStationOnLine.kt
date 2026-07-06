package com.sleekydz86.idolglow.subway.application.port.incoming

import com.sleekydz86.idolglow.subway.domain.SubwayLine
import com.sleekydz86.idolglow.subway.domain.SubwayStationStop

data class SubwayStationOnLine(
    val line: SubwayLine,
    val stop: SubwayStationStop,
)
