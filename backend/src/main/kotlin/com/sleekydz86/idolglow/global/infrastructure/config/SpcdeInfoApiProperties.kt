package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.public-data.spcde")
data class SpcdeInfoApiProperties(
    var baseUrl: String = "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService",
    var serviceKey: String = "",
)
