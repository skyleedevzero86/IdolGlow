package com.sleekydz86.idolglow.admin.adapter.web.dto

data class AdminServerSummaryResponse(
    val cpuUsagePercent: Double?,
    val memoryUsagePercent: Double,
    val diskUsagePercent: Double,
    val jvmHeapUsagePercent: Double,
    val uptimeSeconds: Long,
)
