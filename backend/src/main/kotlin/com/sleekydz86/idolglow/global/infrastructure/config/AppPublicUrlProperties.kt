package com.sleekydz86.idolglow.global.infrastructure.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app")
data class AppPublicUrlProperties(
    var publicBaseUrl: String = "",
)
