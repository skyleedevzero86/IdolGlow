package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.ai.gemma")
data class GemmaProperties(
    var enabled: Boolean = false,
    var baseUrl: String = "http://localhost:8080/v1",
    var apiKey: String = "",
    var model: String = "google/gemma-4-E4B-it",
    var connectTimeoutMs: Int = 10_000,
    var readTimeoutMs: Int = 60_000,
)
