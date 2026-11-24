package com.sleekydz86.idolglow.admin.application

import com.sleekydz86.idolglow.admin.application.dto.AdminActuatorMetricResult
import com.sleekydz86.idolglow.admin.application.dto.AdminActuatorStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminCpuStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminDiskStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminJvmStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminMemoryStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminServerStatusResult
import com.sleekydz86.idolglow.admin.application.dto.AdminServerSummaryResult
import com.sleekydz86.idolglow.admin.application.dto.AdminSystemStatusResult
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.stereotype.Service
import java.lang.management.ManagementFactory
import java.nio.file.Files
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.round

@Service
class AdminServerStatusService(
    private val meterRegistry: MeterRegistry,
    private val infrastructureProbe: InfrastructureProbe,
) {
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
        val infrastructure = infrastructureProbe.probeAll()

        return AdminServerStatusResult(
            generatedAt = OffsetDateTime.now().format(dateTimeFormatter),
            overallStatus = infrastructureProbe.overallStatus(infrastructure),
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

    private fun roundToTwoDecimals(value: Double): Double = round(value * 100.0) / 100.0
}
