package com.sleekydz86.idolglow.servermetric.application.dto

import java.time.LocalDateTime

data class ServerMetricPoint(
    val measuredAt: LocalDateTime,
    val overallStatus: String,
    val dbResponseMs: Long?,
    val minioResponseMs: Long?,
    val httpRequestCount: Long,
    val httpErrorCount: Long,
)

data class ServerMetricTrend(
    val hours: Int,
    val availabilityPercent: Double?,
    val successRatePercent: Double?,
    val averageResponseMs: Long?,
    val points: List<ServerMetricPoint>,
)
