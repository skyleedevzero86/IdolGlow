package com.sleekydz86.idolglow.admin.ui.dto

data class AdminServerStatusResponse(
    val generatedAt: String,
    val overallStatus: String,
    val summary: AdminServerSummaryResponse,
    val system: AdminSystemStatusResponse,
    val infrastructure: List<AdminInfrastructureStatusResponse>,
    val actuator: AdminActuatorStatusResponse,
)

data class AdminServerSummaryResponse(
    val cpuUsagePercent: Double?,
    val memoryUsagePercent: Double,
    val diskUsagePercent: Double,
    val jvmHeapUsagePercent: Double,
    val uptimeSeconds: Long,
)

data class AdminSystemStatusResponse(
    val cpu: AdminCpuStatusResponse,
    val memory: AdminMemoryStatusResponse,
    val disk: AdminDiskStatusResponse,
    val jvm: AdminJvmStatusResponse,
)

data class AdminCpuStatusResponse(
    val systemUsagePercent: Double?,
    val processUsagePercent: Double?,
    val systemLoadAverage: Double?,
    val availableProcessors: Int,
)

data class AdminMemoryStatusResponse(
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val maxBytes: Long,
    val usagePercent: Double,
)

data class AdminDiskStatusResponse(
    val mountPath: String,
    val fileStoreName: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val usedBytes: Long,
    val usagePercent: Double,
)

data class AdminJvmStatusResponse(
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

data class AdminInfrastructureStatusResponse(
    val type: String,
    val label: String,
    val status: String,
    val message: String,
    val responseTimeMs: Long?,
    val details: Map<String, String>,
)

data class AdminActuatorStatusResponse(
    val enabled: Boolean,
    val healthEndpoint: String,
    val metricsEndpoint: String,
    val metrics: List<AdminActuatorMetricResponse>,
)

data class AdminActuatorMetricResponse(
    val name: String,
    val value: Double?,
)
