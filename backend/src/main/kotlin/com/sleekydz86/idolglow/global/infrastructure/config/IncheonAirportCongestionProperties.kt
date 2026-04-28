package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.airport.departure-congestion")
data class IncheonAirportCongestionProperties(
    var baseUrl: String = "https://apis.data.go.kr/B551177/statusOfDepartureCongestion/getDepartureCongestion",
    var serviceKey: String = "",
)
