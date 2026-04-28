package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.subway")
data class SubwayProperties(
    var seoulApiKey: String = "sample",
    var baseUrl: String = "http://openapi.seoul.go.kr:8088",
)
