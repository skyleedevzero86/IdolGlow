package com.sleekydz86.idolglow.eventinfo.application

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.event-meta.stream")
data class EventMetaStreamProperties(
    val enabled: Boolean = false,
    val topic: String = "event-meta.changed",
    val relayBatchSize: Int = 100,
    val sendTimeoutSeconds: Long = 5,
    val retentionDays: Long = 7,
)
