package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.exchange.korea-exim")
data class KoreaEximExchangeProperties(
    var baseUrl: String = "https://oapi.koreaexim.go.kr/site/program/financial",
    var authKey: String = "",
)
