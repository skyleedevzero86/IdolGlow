package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.exchange.naver-directions")
data class NaverDirectionsProperties(
    var baseUrl: String = "https://naveropenapi.apigw.ntruss.com",
    var clientId: String = "",
    var clientSecret: String = "",
    var routeOption: String = "traoptimal",
    var enabled: Boolean = true,
)
