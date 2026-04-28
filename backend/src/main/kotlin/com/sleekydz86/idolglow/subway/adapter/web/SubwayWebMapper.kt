package com.sleekydz86.idolglow.subway.adapter.web

import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayLineDto
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayNearbyDto
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayPageDto
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayStationDto
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwayStationRefDto
import com.sleekydz86.idolglow.subway.adapter.web.dto.SubwaySummaryDto
import com.sleekydz86.idolglow.subway.application.port.incoming.SubwayStationOnLine
import com.sleekydz86.idolglow.subway.application.port.incoming.SubwayStationPageResult
import com.sleekydz86.idolglow.subway.domain.SubwayLine
import com.sleekydz86.idolglow.subway.domain.SubwayPageEnrichment
import com.sleekydz86.idolglow.subway.domain.SubwayStationStop
import org.springframework.stereotype.Component

@Component
class SubwayWebMapper {

    fun toLineDto(line: SubwayLine): SubwayLineDto =
        SubwayLineDto(id = line.id, name = line.name, colorHex = line.colorHex)

    fun toStationDto(on: SubwayStationOnLine): SubwayStationDto =
        SubwayStationDto(
            stationCd = on.stop.code,
            name = on.stop.name,
            frCode = on.stop.frCode,
            lineId = on.line.id,
            lineName = on.line.name,
        )

    fun toRefDto(line: SubwayLine, stop: SubwayStationStop): SubwayStationRefDto =
        SubwayStationRefDto(
            lineId = line.id,
            lineName = line.name,
            stationCd = stop.code,
            name = stop.name,
            frCode = stop.frCode,
        )

    fun toSummaryDto(enrichment: SubwayPageEnrichment): SubwaySummaryDto =
        SubwaySummaryDto(
            title = enrichment.summaryTitle,
            bullets = enrichment.summaryBullets,
            learnMoreLabel = enrichment.learnMoreLabel,
            learnMoreUrl = enrichment.learnMoreUrl,
        )

    fun toNearbyDto(enrichment: SubwayPageEnrichment): SubwayNearbyDto =
        SubwayNearbyDto(
            radiusMeters = enrichment.nearbyRadiusMeters,
            count = enrichment.nearbyCount,
            label = enrichment.nearbyLabel,
        )

    fun toPageDto(result: SubwayStationPageResult): SubwayPageDto =
        SubwayPageDto(
            line = toLineDto(result.line),
            station = toStationDto(SubwayStationOnLine(line = result.line, stop = result.station)),
            prevStation = toRefDto(result.line, result.prevStation),
            nextStation = toRefDto(result.line, result.nextStation),
            summary = toSummaryDto(result.enrichment),
            nearby = toNearbyDto(result.enrichment),
        )
}
