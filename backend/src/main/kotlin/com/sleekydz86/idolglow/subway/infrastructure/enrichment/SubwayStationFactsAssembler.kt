package com.sleekydz86.idolglow.subway.infrastructure.enrichment

import com.sleekydz86.idolglow.subway.application.port.out.SubwayLineCatalogPort
import com.sleekydz86.idolglow.subway.application.port.out.SubwayNearbyPlacePort
import com.sleekydz86.idolglow.subway.application.port.out.SubwayStationLocationPort
import com.sleekydz86.idolglow.subway.application.port.out.SubwayStationOrderPort
import com.sleekydz86.idolglow.subway.domain.SubwayStationFacts
import com.sleekydz86.idolglow.subway.domain.SubwayStationRing
import com.sleekydz86.idolglow.subway.infrastructure.location.ClasspathSubwayStationLocationAdapter
import org.springframework.stereotype.Component

@Component
class SubwayStationFactsAssembler(
    private val lineCatalogPort: SubwayLineCatalogPort,
    private val stationOrderPort: SubwayStationOrderPort,
    private val locationPort: SubwayStationLocationPort,
    private val nearbyPlacePort: SubwayNearbyPlacePort,
) {
    private val linesByStation: Map<String, List<String>> by lazy {
        lineCatalogPort
            .loadAllLines()
            .flatMap { line -> stationOrderPort.orderedStops(line.id).map { normalize(it.name) to line.name } }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, names) -> names.distinct() }
    }

    fun assemble(
        lineId: String,
        lineName: String,
        stationCd: String,
        stationDisplayName: String,
    ): SubwayStationFacts {
        val stops = stationOrderPort.orderedStops(lineId)
        val index = stops.indexOfFirst { it.code == stationCd }
        val neighbors =
            if (index < 0) {
                emptyList()
            } else {
                val ring = SubwayStationRing(stops)
                listOf(ring.previous(index).name, ring.next(index).name).distinct()
            }
        val key = normalize(stationDisplayName)
        return SubwayStationFacts(
            station = key,
            line = lineName,
            transferLines = linesByStation[key].orEmpty().filter { it != lineName },
            neighborStations = neighbors,
            nearbyPlaces = locationPort.locate(stationDisplayName)?.let(nearbyPlacePort::nearby).orEmpty(),
        )
    }

    private fun normalize(name: String): String = ClasspathSubwayStationLocationAdapter.normalize(name)
}
