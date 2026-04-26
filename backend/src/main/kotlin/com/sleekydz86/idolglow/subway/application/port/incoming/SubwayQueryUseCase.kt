package com.sleekydz86.idolglow.subway.application.port.incoming

import com.sleekydz86.idolglow.subway.domain.SubwayLine
import com.sleekydz86.idolglow.subway.domain.SubwayPageEnrichment
import com.sleekydz86.idolglow.subway.domain.SubwayStationStop

data class SubwayStationOnLine(
    val line: SubwayLine,
    val stop: SubwayStationStop,
)

data class SubwayStationPageResult(
    val line: SubwayLine,
    val station: SubwayStationStop,
    val prevStation: SubwayStationStop,
    val nextStation: SubwayStationStop,
    val enrichment: SubwayPageEnrichment,
)

interface SubwayQueryUseCase {
    fun listLines(): List<SubwayLine>
    fun listStations(lineId: String): List<SubwayStationOnLine>
    fun searchStations(query: String): List<SubwayStationOnLine>
    fun getStationPage(lineId: String, stationCd: String): SubwayStationPageResult
}
