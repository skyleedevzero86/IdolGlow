package com.sleekydz86.idolglow.global.infrastructure.config

import io.minio.MinioClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConditionalOnProperty(name = ["app.storage.minio.enabled"], havingValue = "true")
class MinioClientConfig(
    private val minioStorageProperties: MinioStorageProperties,
) {
    @Bean
    fun minioClient(): MinioClient =
        MinioClient.builder()
            .endpoint(minioStorageProperties.endpoint)
            .credentials(minioStorageProperties.accessKey, minioStorageProperties.secretKey)
            .build()
}
