package com.sleekydz86.idolglow.subway.application

import com.sleekydz86.idolglow.subway.application.port.incoming.SubwayQueryUseCase
import com.sleekydz86.idolglow.subway.application.port.incoming.SubwayStationOnLine
import com.sleekydz86.idolglow.subway.application.port.incoming.SubwayStationPageResult
import com.sleekydz86.idolglow.subway.application.port.out.SubwayExternalStationSearchPort
import com.sleekydz86.idolglow.subway.application.port.out.SubwayLineCatalogPort
import com.sleekydz86.idolglow.subway.application.port.out.SubwayPageEnrichmentPort
import com.sleekydz86.idolglow.subway.application.port.out.SubwayStationOrderPort
import com.sleekydz86.idolglow.subway.domain.SeoulMetroLineName
import com.sleekydz86.idolglow.subway.domain.SubwayLine
import com.sleekydz86.idolglow.subway.domain.SubwayStationRing
import com.sleekydz86.idolglow.subway.domain.SubwayStationStop
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.web.server.ResponseStatusException

@Service
class SubwayQueryApplicationService(
    private val lineCatalogPort: SubwayLineCatalogPort,
    private val stationOrderPort: SubwayStationOrderPort,
    private val externalStationSearchPort: SubwayExternalStationSearchPort,
    private val pageEnrichmentPort: SubwayPageEnrichmentPort,
) : SubwayQueryUseCase {

    private val linesById: Map<String, SubwayLine> by lazy {
        lineCatalogPort.loadAllLines().associateBy { it.id }
    }

    override fun listLines(): List<SubwayLine> =
        lineCatalogPort.loadAllLines()

    override fun listStations(lineId: String): List<SubwayStationOnLine> {
        val line = linesById[lineId] ?: return emptyList()
        return stationOrderPort.orderedStops(lineId).map { SubwayStationOnLine(line = line, stop = it) }
    }

    override fun searchStations(query: String): List<SubwayStationOnLine> {
        val q = query.trim()
        if (q.isEmpty()) {
            return emptyList()
        }
        val hits = LinkedHashMap<String, SubwayStationOnLine>()
        for (line in lineCatalogPort.loadAllLines()) {
            for (stop in stationOrderPort.orderedStops(line.id)) {
                if (stop.name.contains(q, ignoreCase = true)) {
                    hits.putIfAbsent("${line.id}:${stop.code}", SubwayStationOnLine(line = line, stop = stop))
                }
            }
        }
        if (q.length >= 2) {
            for (remote in externalStationSearchPort.searchByStationName(q)) {
                val lineId = SeoulMetroLineName.parseNumericLineId(remote.lineNumLabel) ?: continue
                val line = linesById[lineId] ?: continue
                hits.putIfAbsent(
                    "$lineId:${remote.stationCd}",
                    SubwayStationOnLine(
                        line = line,
                        stop = SubwayStationStop(
                            code = remote.stationCd,
                            name = remote.stationName,
                            frCode = remote.frCode.ifEmpty { remote.stationCd.takeLast(3) },
                        ),
                    ),
                )
            }
        }
        return hits.values.take(30)
    }

    override fun getStationPage(lineId: String, stationCd: String): SubwayStationPageResult {
        val line = linesById[lineId] ?: throw ResponseStatusException(HttpStatus.NOT_FOUND, "존재하지 않는 노선입니다.")
        val stops = stationOrderPort.orderedStops(lineId)
        if (stops.isEmpty()) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "이 노선에 등록된 역 순서 데이터가 없습니다.")
        }
        val ring = SubwayStationRing(stops)
        val idx = ring.indexOf(stationCd)
        if (idx < 0) {
            throw ResponseStatusException(HttpStatus.NOT_FOUND, "해당 노선에 속하지 않는 역입니다.")
        }
        val current = ring.at(idx)
        val enrichment = pageEnrichmentPort.loadFor(line.id, line.name, stationCd, current.name)
        return SubwayStationPageResult(
            line = line,
            station = current,
            prevStation = ring.previous(idx),
            nextStation = ring.next(idx),
            enrichment = enrichment,
        )
    }
}
