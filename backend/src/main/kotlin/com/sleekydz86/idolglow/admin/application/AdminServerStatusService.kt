package com.sleekydz86.idolglow.admin.application

import com.sleekydz86.idolglow.admin.application.dto.AdminActuatorMetricResult
import com.sleekydz86.idolglow.admin.application.dto.AdminActuatorStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminCpuStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminDiskStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminInfrastructureStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminJvmStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminMemoryStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminServerStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminServerSummaryResult
import com.sleekydz86.idolglow.admin.application.dto.AdminSystemStatusResult
import com.sleekydz86.idolglow.global.config.MinioStorageProperties
import io.micrometer.core.instrument.MeterRegistry
import io.minio.BucketExistsArgs
import io.minio.MinioClient
import org.springframework.beans.factory.ObjectProvider
import org.springframework.core.env.Environment
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.sql.DataSource
import kotlin.math.round

@Service
class AdminServerStatusService(
    private val dataSource: DataSource,
    private val meterRegistry: MeterRegistry,
    private val environment: Environment,
    private val minioStorageProperties: MinioStorageProperties,
    minioClientProvider: ObjectProvider<MinioClient>,
) {
    private val minioClient: MinioClient? = minioClientProvider.ifAvailable
    private val runtime = Runtime.getRuntime()
    private val memoryBean = ManagementFactory.getMemoryMXBean()
    private val runtimeBean = ManagementFactory.getRuntimeMXBean()
    private val threadBean = ManagementFactory.getThreadMXBean()
    private val dateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

    fun getServerStatus(): AdminServerStatusResult {
        val memory = memoryStatus()
        val disk = diskStatus()
        val jvm = jvmStatus()
        val cpu = cpuStatus()
        val infrastructure =
            listOf(
                databaseStatus(),
                minioStatus(),
                redisStatus(),
                mqStatus(),
            )

        return AdminServerStatusResult(
            generatedAt = OffsetDateTime.now().format(dateTimeFormatter),
            overallStatus = resolveOverallStatus(infrastructure),
            summary =
                AdminServerSummaryResult(
                    cpuUsagePercent = cpu.systemUsagePercent,
                    memoryUsagePercent = memory.usagePercent,
                    diskUsagePercent = disk.usagePercent,
                    jvmHeapUsagePercent = jvm.heapUsagePercent,
                    uptimeSeconds = jvm.uptimeSeconds,
                ),
            system =
                AdminSystemStatusResult(
                    cpu = cpu,
                    memory = memory,
                    disk = disk,
                    jvm = jvm,
                ),
            infrastructure = infrastructure,
            actuator =
                AdminActuatorStatusResult(
                    enabled = true,
                    healthEndpoint = "/actuator/health",
                    metricsEndpoint = "/actuator/metrics",
                    metrics =
                        listOf(
                            AdminActuatorMetricResult(
                                name = "system.cpu.usage",
                                value = meterPercent("system.cpu.usage"),
                            ),
                            AdminActuatorMetricResult(
                                name = "process.cpu.usage",
                                value = meterPercent("process.cpu.usage"),
                            ),
                            AdminActuatorMetricResult(
                                name = "system.load.average.1m",
                                value = meterValue("system.load.average.1m"),
                            ),
                            AdminActuatorMetricResult(
                                name = "jvm.threads.live",
                                value = meterValue("jvm.threads.live"),
                            ),
                            AdminActuatorMetricResult(
                                name = "hikaricp.connections.active",
                                value = meterValue("hikaricp.connections.active", "jdbc.connections.active"),
                            ),
                            AdminActuatorMetricResult(
                                name = "hikaricp.connections.max",
                                value = meterValue("hikaricp.connections.max", "jdbc.connections.max"),
                            ),
                        ),
                ),
        )
    }

    private fun cpuStatus(): AdminCpuStatusResult =
        AdminCpuStatusResult(
            systemUsagePercent = meterPercent("system.cpu.usage"),
            processUsagePercent = meterPercent("process.cpu.usage"),
            systemLoadAverage = meterValue("system.load.average.1m"),
            availableProcessors = runtime.availableProcessors(),
        )

    private fun memoryStatus(): AdminMemoryStatusResult {
        val maxBytes = runtime.maxMemory().coerceAtLeast(1L)
        val totalBytes = runtime.totalMemory()
        val freeBytes = runtime.freeMemory()
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

        return AdminMemoryStatusResult(
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedBytes = usedBytes,
            maxBytes = maxBytes,
            usagePercent = percentage(usedBytes, maxBytes),
        )
    }

    private fun diskStatus(): AdminDiskStatusResult {
        val rootPath = Path.of(System.getProperty("user.dir")).toAbsolutePath().root ?: Path.of("/")
        val fileStore = Files.getFileStore(rootPath)
        val totalBytes = fileStore.totalSpace
        val freeBytes = fileStore.usableSpace
        val usedBytes = (totalBytes - freeBytes).coerceAtLeast(0L)

        return AdminDiskStatusResult(
            mountPath = rootPath.toString(),
            fileStoreName = fileStore.name(),
            totalBytes = totalBytes,
            freeBytes = freeBytes,
            usedBytes = usedBytes,
            usagePercent = percentage(usedBytes, totalBytes),
        )
    }

    private fun jvmStatus(): AdminJvmStatusResult {
        val heapUsage = memoryBean.heapMemoryUsage
        val nonHeapUsage = memoryBean.nonHeapMemoryUsage
        val heapMax = heapUsage.max.takeIf { it > 0 } ?: heapUsage.committed.coerceAtLeast(1L)

        return AdminJvmStatusResult(
            heapUsedBytes = heapUsage.used,
            heapCommittedBytes = heapUsage.committed,
            heapMaxBytes = heapMax,
            heapUsagePercent = percentage(heapUsage.used, heapMax),
            nonHeapUsedBytes = nonHeapUsage.used,
            nonHeapCommittedBytes = nonHeapUsage.committed,
            liveThreadCount = threadBean.threadCount,
            daemonThreadCount = threadBean.daemonThreadCount,
            peakThreadCount = threadBean.peakThreadCount,
            uptimeSeconds = Duration.ofMillis(runtimeBean.uptime).seconds,
            startTime =
                Instant
                    .ofEpochMilli(runtimeBean.startTime)
                    .atZone(ZoneId.systemDefault())
                    .toOffsetDateTime()
                    .format(dateTimeFormatter),
        )
    }

    private fun databaseStatus(): AdminInfrastructureStatusResult {
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

    private fun minioStatus(): AdminInfrastructureStatusResult {
        if (!minioStorageProperties.enabled || minioClient == null) {
            return AdminInfrastructureStatusResult(
                type = "MINIO",
                label = "MinIO",
                status = "NOT_CONFIGURED",
                message = "설정에서 MinIO가 비활성화되어 있습니다.",
                responseTimeMs = null,
                details =
                    linkedMapOf(
                        "enabled" to minioStorageProperties.enabled.toString(),
                        "endpoint" to minioStorageProperties.endpoint.ifBlank { "-" },
                        "bucket" to minioStorageProperties.bucket.ifBlank { "-" },
                    ),
            )
        }

        val client =
            minioClient ?: return AdminInfrastructureStatusResult(
                type = "MINIO",
                label = "MinIO",
                status = "NOT_CONFIGURED",
                message = "MinIO 클라이언트 빈을 사용할 수 없습니다.",
                responseTimeMs = null,
                details =
                    linkedMapOf(
                        "enabled" to minioStorageProperties.enabled.toString(),
                        "endpoint" to minioStorageProperties.endpoint.ifBlank { "-" },
                        "bucket" to minioStorageProperties.bucket.ifBlank { "-" },
                    ),
            )
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
                        "publicReadWebzineObjects" to minioStorageProperties.publicReadWebzineObjects.toString(),
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

    private fun resolveOverallStatus(infrastructure: List<AdminInfrastructureStatusResult>): String =
        when {
            infrastructure.any { it.status == "DOWN" } -> "DOWN"
            infrastructure.any { it.status == "UNKNOWN" } -> "DEGRADED"
            else -> "UP"
        }

    private fun meterPercent(vararg names: String): Double? = meterValue(*names)?.let { roundToTwoDecimals(it * 100.0) }

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
            }?.let(::roundToTwoDecimals)

    private fun percentage(
        numerator: Long,
        denominator: Long,
    ): Double {
        val safeDenominator = denominator.coerceAtLeast(1L)
        return roundToTwoDecimals(numerator.toDouble() / safeDenominator.toDouble() * 100.0)
    }

    private fun currentProperty(
        name: String,
        fallback: String,
    ): String = environment.getProperty(name)?.takeIf { it.isNotBlank() } ?: fallback

    private fun formatMetricValue(value: Double?): String = value?.toString() ?: "-"

    private fun elapsedMillis(startedAt: Long): Long = Duration.ofNanos(System.nanoTime() - startedAt).toMillis()

    private fun roundToTwoDecimals(value: Double): Double = round(value * 100.0) / 100.0
}
