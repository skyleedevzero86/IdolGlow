package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.exchange.airport-hub")
data class ExchangeAirportHubProperties(
    var longitude: Double = 126.4407,
    var latitude: Double = 37.4602,
)
