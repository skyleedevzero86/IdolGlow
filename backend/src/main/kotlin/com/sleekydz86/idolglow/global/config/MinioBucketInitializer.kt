package com.sleekydz86.idolglow.global.config

import io.minio.BucketExistsArgs
import io.minio.MakeBucketArgs
import io.minio.MinioClient
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
@ConditionalOnBean(MinioClient::class)
class MinioBucketInitializer(
    private val minioClient: MinioClient,
    private val minioStorageProperties: MinioStorageProperties,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @EventListener(ApplicationReadyEvent::class)
    fun ensureBucket() {
        val bucket = minioStorageProperties.bucket
        try {
            val exists = minioClient.bucketExists(
                BucketExistsArgs.builder().bucket(bucket).build()
            )
            if (!exists) {
                minioClient.makeBucket(MakeBucketArgs.builder().bucket(bucket).build())
                log.info("MinIO 버킷 생성: {}", bucket)
            }
        } catch (e: Exception) {
            log.warn("MinIO 버킷 확인/생성 실패 (bucket={}): {}", bucket, e.message)
        }
    }
}
