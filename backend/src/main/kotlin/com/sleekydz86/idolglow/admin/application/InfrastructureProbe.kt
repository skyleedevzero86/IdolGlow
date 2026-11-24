package com.sleekydz86.idolglow.admin.application

import com.sleekydz86.idolglow.admin.application.dto.AdminInfrastructureStatusResult
import com.sleekydz86.idolglow.global.config.MinioStorageProperties
import io.micrometer.core.instrument.MeterRegistry
import io.minio.BucketExistsArgs
import io.minio.MinioClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import java.time.Duration
import javax.sql.DataSource
import kotlin.math.round

@Component
class InfrastructureProbe(
    private val dataSource: DataSource,
    private val meterRegistry: MeterRegistry,
    private val environment: Environment,
    private val minioStorageProperties: MinioStorageProperties,
    minioClientProvider: ObjectProvider<MinioClient>,
) {
    private val minioClient: MinioClient? = minioClientProvider.ifAvailable

    fun probeAll(): List<AdminInfrastructureStatusResult> =
        listOf(
            databaseStatus(),
            minioStatus(),
            redisStatus(),
            mqStatus(),
        )

    fun overallStatus(infrastructure: List<AdminInfrastructureStatusResult>): String =
        when {
            infrastructure.any { it.status == "DOWN" } -> "DOWN"
            infrastructure.any { it.status == "UNKNOWN" } -> "DEGRADED"
            else -> "UP"
        }

    fun databaseStatus(): AdminInfrastructureStatusResult {
        val startedAt = System.nanoTime()

        return runCatching {
            dataSource.connection.use { connection ->
                val metadata = connection.metaData
                val valid = runCatching { connection.isValid(2) }.getOrDefault(true)
                val responseTimeMs = elapsedMillis(startedAt)
                val activeConnections = meterValue("hikaricp.connections.active", "jdbc.connections.active")
                val idleConnections = meterValue("hikaricp.connections.idle", "jdbc.connections.idle")
                val maxConnections = meterValue("hikaricp.connections.max", "jdbc.connections.max")

                AdminInfrastructureStatusResult(
                    type = "DATABASE",
                    label = "데이터베이스",
                    status = if (valid) "UP" else "DOWN",
                    message = if (valid) "데이터베이스 연결이 정상입니다." else "데이터베이스 유효성 검사에 실패했습니다.",
                    responseTimeMs = responseTimeMs,
                    details =
                        linkedMapOf(
                            "url" to currentProperty("spring.datasource.url", metadata.url),
                            "product" to metadata.databaseProductName,
                            "version" to metadata.databaseProductVersion,
                            "driver" to metadata.driverName,
                            "catalog" to (connection.catalog ?: "-"),
                            "activeConnections" to formatMetricValue(activeConnections),
                            "idleConnections" to formatMetricValue(idleConnections),
                            "maxConnections" to formatMetricValue(maxConnections),
                        ),
                )
            }
        }.getOrElse { error ->
            AdminInfrastructureStatusResult(
                type = "DATABASE",
                label = "데이터베이스",
                status = "DOWN",
                message = error.message ?: "데이터베이스 연결 확인에 실패했습니다.",
                responseTimeMs = elapsedMillis(startedAt),
                details =
                    linkedMapOf(
                        "url" to currentProperty("spring.datasource.url", "-"),
                        "driver" to currentProperty("spring.datasource.driver-class-name", "-"),
                    ),
            )
        }
    }

    fun minioStatus(): AdminInfrastructureStatusResult {
        if (!minioStorageProperties.enabled || minioClient == null) {
            return minioNotConfigured("설정에서 MinIO가 비활성화되어 있습니다.")
        }

        val client = minioClient ?: return minioNotConfigured("MinIO 클라이언트 빈을 사용할 수 없습니다.")
        val startedAt = System.nanoTime()

        return runCatching {
            val bucketExists =
                client.bucketExists(
                    BucketExistsArgs.builder().bucket(minioStorageProperties.bucket).build(),
                )

            AdminInfrastructureStatusResult(
                type = "MINIO",
                label = "MinIO",
                status = if (bucketExists) "UP" else "DOWN",
                message = if (bucketExists) "MinIO 버킷 연결이 정상입니다." else "설정된 MinIO 버킷을 찾을 수 없습니다.",
                responseTimeMs = elapsedMillis(startedAt),
                details =
                    linkedMapOf(
                        "endpoint" to minioStorageProperties.endpoint,
                        "bucket" to minioStorageProperties.bucket,
                        "publicBaseUrl" to minioStorageProperties.publicBaseUrl.ifBlank { "-" },
                        "publicReadProfileObjects" to minioStorageProperties.publicReadProfileObjects.toString(),
                        "publicReadIdolGlowObjects" to minioStorageProperties.publicReadIdolGlowObjects.toString(),
                    ),
            )
        }.getOrElse { error ->
            AdminInfrastructureStatusResult(
                type = "MINIO",
                label = "MinIO",
                status = "DOWN",
                message = error.message ?: "MinIO 연결 확인에 실패했습니다.",
                responseTimeMs = elapsedMillis(startedAt),
                details =
                    linkedMapOf(
                        "endpoint" to minioStorageProperties.endpoint.ifBlank { "-" },
                        "bucket" to minioStorageProperties.bucket.ifBlank { "-" },
                        "publicBaseUrl" to minioStorageProperties.publicBaseUrl.ifBlank { "-" },
                    ),
            )
        }
    }

    private fun minioNotConfigured(message: String): AdminInfrastructureStatusResult =
        AdminInfrastructureStatusResult(
            type = "MINIO",
            label = "MinIO",
            status = "NOT_CONFIGURED",
            message = message,
            responseTimeMs = null,
            details =
                linkedMapOf(
                    "enabled" to minioStorageProperties.enabled.toString(),
                    "endpoint" to minioStorageProperties.endpoint.ifBlank { "-" },
                    "bucket" to minioStorageProperties.bucket.ifBlank { "-" },
                ),
        )

    private fun redisStatus(): AdminInfrastructureStatusResult {
        val host = currentProperty("spring.data.redis.host", currentProperty("spring.redis.host", ""))
        val port = currentProperty("spring.data.redis.port", currentProperty("spring.redis.port", ""))
        val configured = host.isNotBlank()

        return AdminInfrastructureStatusResult(
            type = "REDIS",
            label = "Redis",
            status = if (configured) "UNKNOWN" else "NOT_CONFIGURED",
            message =
                if (configured) {
                    "Redis 설정은 감지되었으나 이 프로젝트에는 Redis 클라이언트 또는 헬스 체크가 연결되어 있지 않습니다."
                } else {
                    "Redis가 구성되어 있지 않습니다."
                },
            responseTimeMs = null,
            details =
                linkedMapOf(
                    "configuredHost" to host.ifBlank { "-" },
                    "configuredPort" to port.ifBlank { "-" },
                    "starterPresent" to "false",
                ),
        )
    }

    private fun mqStatus(): AdminInfrastructureStatusResult {
        val rabbitHost = currentProperty("spring.rabbitmq.host", "")
        val kafkaServers = currentProperty("spring.kafka.bootstrap-servers", "")
        val activeMqBroker = currentProperty("spring.activemq.broker-url", "")
        val configured = rabbitHost.isNotBlank() || kafkaServers.isNotBlank() || activeMqBroker.isNotBlank()

        return AdminInfrastructureStatusResult(
            type = "MQ",
            label = "메시지 큐",
            status = if (configured) "UNKNOWN" else "NOT_CONFIGURED",
            message =
                if (configured) {
                    "메시지 큐 설정은 감지되었으나 브로커 헬스 체크가 이 프로젝트에 연결되어 있지 않습니다."
                } else {
                    "RabbitMQ, Kafka, ActiveMQ가 구성되어 있지 않습니다."
                },
            responseTimeMs = null,
            details =
                linkedMapOf(
                    "rabbitmqHost" to rabbitHost.ifBlank { "-" },
                    "kafkaBootstrapServers" to kafkaServers.ifBlank { "-" },
                    "activeMqBrokerUrl" to activeMqBroker.ifBlank { "-" },
                    "starterPresent" to "false",
                ),
        )
    }

    private fun meterValue(vararg names: String): Double? =
        names
            .firstNotNullOfOrNull { name ->
                meterRegistry
                    .find(name)
                    .meters()
                    .firstOrNull()
                    ?.measure()
                    ?.firstOrNull()
                    ?.value
            }?.let { round(it * 100.0) / 100.0 }

    private fun currentProperty(
        name: String,
        fallback: String,
    ): String = environment.getProperty(name)?.takeIf { it.isNotBlank() } ?: fallback

    private fun formatMetricValue(value: Double?): String = value?.toString() ?: "-"

    private fun elapsedMillis(startedAt: Long): Long = Duration.ofNanos(System.nanoTime() - startedAt).toMillis()
}
