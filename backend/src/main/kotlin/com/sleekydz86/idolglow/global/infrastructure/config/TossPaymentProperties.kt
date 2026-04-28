package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "payment.toss")
data class TossPaymentProperties(
    var enabled: Boolean = false,
    var secretKey: String = "",
    var clientKey: String = "",
    var baseUrl: String = "https://api.tosspayments.com",
    var webhookSecret: String = "",
    var connectTimeoutMs: Int = 10_000,
    var readTimeoutMs: Int = 30_000,
    var useTossProvider: Boolean = false,
)
