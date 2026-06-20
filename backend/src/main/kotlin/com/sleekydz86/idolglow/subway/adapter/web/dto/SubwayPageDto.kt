package com.sleekydz86.idolglow.subway.adapter.web.dto

data class SubwayPageDto(
    val line: SubwayLineDto,
    val station: SubwayStationDto,
    val prevStation: SubwayStationRefDto,
    val nextStation: SubwayStationRefDto,
    val summary: SubwaySummaryDto,
    val nearby: SubwayNearbyDto,
)
