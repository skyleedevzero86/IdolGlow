package com.sleekydz86.idolglow.eventinfo.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.event-search")
data class EventSearchProperties(
    val enabled: Boolean = false,
    val baseUrl: String = "http://localhost:9200",
    val index: String = "idolglow-event-meta",
    val timeoutSeconds: Long = 5,
    val defaultSize: Int = 20,
    val maxSize: Int = 50,
)
