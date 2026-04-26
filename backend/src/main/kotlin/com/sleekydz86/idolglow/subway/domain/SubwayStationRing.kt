package com.sleekydz86.idolglow.subway.domain

class SubwayStationRing(
    stops: List<SubwayStationStop>,
) {
    private val ordered: List<SubwayStationStop> = stops.toList()

    init {
        require(ordered.isNotEmpty()) { "순환 역 목록은 비어 있을 수 없습니다." }
    }

    fun indexOf(stationCode: String): Int = ordered.indexOfFirst { it.code == stationCode }

    fun at(index: Int): SubwayStationStop = ordered[index]

    fun previous(index: Int): SubwayStationStop =
        if (index == 0) ordered.last() else ordered[index - 1]

    fun next(index: Int): SubwayStationStop =
        if (index == ordered.lastIndex) ordered.first() else ordered[index + 1]

    val size: Int get() = ordered.size
}
