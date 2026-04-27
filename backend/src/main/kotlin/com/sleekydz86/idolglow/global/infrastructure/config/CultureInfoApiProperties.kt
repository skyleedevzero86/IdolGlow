package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.public-data.culture-info")
data class CultureInfoApiProperties(
    var baseUrl: String = "https://apis.data.go.kr/B553457/cultureinfo",
    var serviceKey: String = "",
    var livelihoodDateParam: String = "stdDate",
)
