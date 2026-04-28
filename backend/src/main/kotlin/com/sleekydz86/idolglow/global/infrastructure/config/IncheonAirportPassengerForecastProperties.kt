package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.airport.passgr-anncmt")
data class IncheonAirportPassengerForecastProperties(
    var baseUrl: String = "https://apis.data.go.kr/B551177/passgrAnncmt/getPassgrAnncmt",
    var serviceKey: String = "",
)
