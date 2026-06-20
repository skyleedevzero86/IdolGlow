package com.sleekydz86.idolglow.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.tour.kor-api")
data class TourKorApiProperties(
    var baseUrl: String = "https://apis.data.go.kr/B551011/KorService2",
    var serviceKey: String = "",
    var mobileOs: String = "WEB",
    var mobileApp: String = "idolglow",
)
