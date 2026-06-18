package com.sleekydz86.idolglow.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.calendar")
data class AppCalendarProperties(
    var eventZoneId: String = "Asia/Seoul",
    var uidDomain: String = "idolglow.app",
)
