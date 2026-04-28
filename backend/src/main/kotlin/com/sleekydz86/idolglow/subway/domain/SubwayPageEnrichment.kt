package com.sleekydz86.idolglow.subway.domain

data class SubwayPageEnrichment(
    val summaryTitle: String,
    val summaryBullets: List<String>,
    val learnMoreLabel: String,
    val learnMoreUrl: String?,
    val nearbyRadiusMeters: Int,
    val nearbyCount: Int,
    val nearbyLabel: String,
)
