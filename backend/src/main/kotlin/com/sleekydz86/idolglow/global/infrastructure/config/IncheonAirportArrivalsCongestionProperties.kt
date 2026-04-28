package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.airport.arrivals-congestion")
data class IncheonAirportArrivalsCongestionProperties(
    val baseUrl: String = "http://apis.data.go.kr/B551177/StatusOfArrivals/getArrivalsCongestion",
    val serviceKey: String = "",
)
