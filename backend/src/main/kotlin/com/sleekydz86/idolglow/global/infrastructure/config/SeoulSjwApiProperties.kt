package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.seoul.sjw-api")
data class SeoulSjwApiProperties(
    var baseUrl: String = "http://openapi.seoul.go.kr:8088",
    var apiKey: String = "",
)
