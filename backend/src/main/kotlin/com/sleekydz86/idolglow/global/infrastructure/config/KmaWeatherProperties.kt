package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.weather.kma")
data class KmaWeatherProperties(
    var serviceKey: String = "",
    var villageBaseUrl: String = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0",
    var midBaseUrl: String = "https://apis.data.go.kr/1360000/MidFcstInfoService",
    var zoneBaseUrl: String = "https://apis.data.go.kr/1360000/FcstZoneInfoService",
    var asosBaseUrl: String = "https://apis.data.go.kr/1360000/AsosDalyInfoService",
)
