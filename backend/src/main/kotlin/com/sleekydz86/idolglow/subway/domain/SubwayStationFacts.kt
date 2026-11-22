package com.sleekydz86.idolglow.subway.domain

data class SubwayNearbyPlace(
    val name: String,
    val category: String,
    val distanceMeters: Int,
)

data class SubwayStationFacts(
    val station: String,
    val line: String,
    val transferLines: List<String>,
    val neighborStations: List<String>,
    val nearbyPlaces: List<SubwayNearbyPlace>,
)
