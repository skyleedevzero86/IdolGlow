package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.airport.parking")
data class IncheonAirportParkingProperties(
    val baseUrl: String = "http://apis.data.go.kr/B551177/StatusOfParking/getTrackingParking",
    val serviceKey: String = "",
)
