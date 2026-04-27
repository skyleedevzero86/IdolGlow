package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.kopis.api")
data class KopisApiProperties(
    var baseUrl: String = "http://www.kopis.or.kr/openApi/restful",
    var serviceKey: String = "",
)
