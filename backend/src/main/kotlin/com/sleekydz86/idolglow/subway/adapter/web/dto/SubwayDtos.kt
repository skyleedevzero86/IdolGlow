package com.sleekydz86.idolglow.subway.adapter.web.dto

data class SubwayLineDto(
    val id: String,
    val name: String,
    val colorHex: String,
)

data class SubwayStationDto(
    val stationCd: String,
    val name: String,
    val frCode: String,
    val lineId: String,
    val lineName: String,
)

data class SubwayStationRefDto(
    val lineId: String,
    val lineName: String,
    val stationCd: String,
    val name: String,
    val frCode: String,
)

data class SubwaySummaryDto(
    val title: String,
    val bullets: List<String>,
    val learnMoreLabel: String,
    val learnMoreUrl: String?,
)

data class SubwayNearbyDto(
    val radiusMeters: Int,
    val count: Int,
    val label: String,
)

data class SubwayPageDto(
    val line: SubwayLineDto,
    val station: SubwayStationDto,
    val prevStation: SubwayStationRefDto,
    val nextStation: SubwayStationRefDto,
    val summary: SubwaySummaryDto,
    val nearby: SubwayNearbyDto,
)
