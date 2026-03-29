package com.sleekydz86.idolglow.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "app.storage.minio")
data class MinioStorageProperties(
    var enabled: Boolean = false,
    var endpoint: String = "",
    var accessKey: String = "",
    var secretKey: String = "",
    var bucket: String = "",
    var publicBaseUrl: String = "",
    var publicReadProfileObjects: Boolean = false,
)
