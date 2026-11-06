package com.sleekydz86.idolglow.global.llm

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.llm.local")
data class LocalLlmProperties(
    var enabled: Boolean = false,
    var baseUrl: String = "",
    var model: String = "",
    var timeoutSeconds: Long = 20,
)

@ConfigurationProperties(prefix = "app.llm.gateway")
data class LlmGatewayProperties(
    var externalEnabled: Boolean = false,
    var externalDailyLimit: Int = 200,
    var chains: Map<String, List<String>> = mapOf("default" to listOf("local")),
)
