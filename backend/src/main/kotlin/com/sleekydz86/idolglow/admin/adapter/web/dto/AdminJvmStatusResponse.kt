package com.sleekydz86.idolglow.admin.ui.dto

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
