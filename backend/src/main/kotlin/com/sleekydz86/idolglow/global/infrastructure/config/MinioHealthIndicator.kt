package com.sleekydz86.idolglow.global.infrastructure.config

import io.minio.BucketExistsArgs
import io.minio.MinioClient
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.health.contributor.Health
import org.springframework.boot.health.contributor.HealthIndicator
import org.springframework.stereotype.Component

@Component("minio")
@ConditionalOnBean(MinioClient::class)
class MinioHealthIndicator(
    private val minioClient: MinioClient,
    private val minioStorageProperties: MinioStorageProperties,
) : HealthIndicator {

    override fun health(): Health {
        val bucket = minioStorageProperties.bucket

        return runCatching {
            val exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
            )
            if (exists) {
                Health.up()
                    .withDetail("endpoint", minioStorageProperties.endpoint)
                    .withDetail("bucket", bucket)
                    .withDetail("publicBaseUrl", minioStorageProperties.publicBaseUrl)
                    .build()
            } else {
                Health.down()
                    .withDetail("endpoint", minioStorageProperties.endpoint)
                    .withDetail("bucket", bucket)
                    .withDetail("message", "설정한 버킷을 찾을 수 없습니다.")
                    .build()
            }
        }.getOrElse { error ->
            Health.down(error)
                .withDetail("endpoint", minioStorageProperties.endpoint)
                .withDetail("bucket", bucket)
                .build()
        }
    }
}
