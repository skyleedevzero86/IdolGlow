package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.tour.api")
data class TourApiProperties(
    var baseUrl: String = "https://apis.data.go.kr/B551011/LocgoHubTarService1",
    var serviceKey: String = "",
    var mobileOs: String = "WEB",
    var mobileApp: String = "idolglow",
    var cacheTtlMinutes: Long = 360,
    var cacheMaxEntries: Int = 300,
)
