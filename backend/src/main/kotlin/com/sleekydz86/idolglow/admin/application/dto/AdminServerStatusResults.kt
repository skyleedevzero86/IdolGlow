package com.sleekydz86.idolglow.admin.application.dto

data class AdminServerStatusResult(
    val generatedAt: String,
    val overallStatus: String,
    val summary: AdminServerSummaryResult,
    val system: AdminSystemStatusResult,
    val infrastructure: List<AdminInfrastructureStatusResult>,
    val actuator: AdminActuatorStatusResult,
)

data class AdminServerSummaryResult(
    val cpuUsagePercent: Double?,
    val memoryUsagePercent: Double,
    val diskUsagePercent: Double,
    val jvmHeapUsagePercent: Double,
    val uptimeSeconds: Long,
)

data class AdminSystemStatusResult(
    val cpu: AdminCpuStatusResult,
    val memory: AdminMemoryStatusResult,
    val disk: AdminDiskStatusResult,
    val jvm: AdminJvmStatusResult,
)

data class AdminCpuStatusResult(
    val systemUsagePercent: Double?,
    val processUsagePercent: Double?,
    val systemLoadAverage: Double?,
    val availableProcessors: Int,
)

data class AdminMemoryStatusResult(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val maxBytes: Long,
    val usagePercent: Double,
)

data class AdminDiskStatusResult(
    val mountPath: String,
    val fileStoreName: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val usagePercent: Double,
)

data class AdminJvmStatusResult(
    val heapUsedBytes: Long,
    val heapCommittedBytes: Long,
    val heapMaxBytes: Long,
    val heapUsagePercent: Double,
    val nonHeapUsedBytes: Long,
    val nonHeapCommittedBytes: Long,
    val liveThreadCount: Int,
    val daemonThreadCount: Int,
    val peakThreadCount: Int,
    val uptimeSeconds: Long,
    val startTime: String,
)

data class AdminInfrastructureStatusResult(
    val type: String,
    val label: String,
    val status: String,
    val message: String,
    val responseTimeMs: Long?,
    val details: Map<String, String>,
)

data class AdminActuatorStatusResult(
    val enabled: Boolean,
    val healthEndpoint: String,
    val metricsEndpoint: String,
    val metrics: List<AdminActuatorMetricResult>,
)

data class AdminActuatorMetricResult(
    val name: String,
    val value: Double?,
)
