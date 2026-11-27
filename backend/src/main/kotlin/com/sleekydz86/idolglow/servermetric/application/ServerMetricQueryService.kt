package com.sleekydz86.idolglow.servermetric.application

import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.servermetric.application.dto.ServerMetricPoint
import com.sleekydz86.idolglow.servermetric.application.dto.ServerMetricTrend
import com.sleekydz86.idolglow.servermetric.application.port.out.ServerMetricPort
import com.sleekydz86.idolglow.servermetric.domain.ServerMetricSnapshot
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode

@Service
@Transactional(readOnly = true)
class ServerMetricQueryService(
    private val serverMetricPort: ServerMetricPort,
    private val dashboardTime: DashboardTime,
) {
    fun trend(requestedHours: Int?): ServerMetricTrend {
        val hours = clampHours(requestedHours)
        val snapshots = serverMetricPort.findMeasuredSince(dashboardTime.hoursAgo(hours.toLong()))
        return ServerMetricTrend(
            hours = hours,
            availabilityPercent = availability(snapshots),
            successRatePercent = successRate(snapshots),
            averageResponseMs = averageResponse(snapshots),
            points = snapshots.map(::toPoint),
        )
    }

    private fun availability(snapshots: List<ServerMetricSnapshot>): Double? {
        if (snapshots.isEmpty()) return null
        return percent(snapshots.count { it.isUp() }.toLong(), snapshots.size.toLong())
    }

    private fun successRate(snapshots: List<ServerMetricSnapshot>): Double? {
        val total = snapshots.sumOf { it.httpRequestCount }
        if (total == 0L) return null
        return percent(total - snapshots.sumOf { it.httpErrorCount }, total)
    }

    private fun averageResponse(snapshots: List<ServerMetricSnapshot>): Long? {
        val measured = snapshots.mapNotNull { it.dbResponseMs }
        if (measured.isEmpty()) return null
        return BigDecimal(measured.sum())
            .divide(BigDecimal(measured.size), 0, RoundingMode.HALF_UP)
            .toLong()
    }

    private fun percent(
        part: Long,
        whole: Long,
    ): Double =
        BigDecimal(part)
            .multiply(HUNDRED)
            .divide(BigDecimal(whole), 1, RoundingMode.HALF_UP)
            .toDouble()

    private fun toPoint(snapshot: ServerMetricSnapshot): ServerMetricPoint =
        ServerMetricPoint(
            measuredAt = snapshot.measuredAt,
            overallStatus = snapshot.overallStatus,
            dbResponseMs = snapshot.dbResponseMs,
            minioResponseMs = snapshot.minioResponseMs,
            httpRequestCount = snapshot.httpRequestCount,
            httpErrorCount = snapshot.httpErrorCount,
        )

    companion object {
        const val DEFAULT_HOURS: Int = 24
        const val MIN_HOURS: Int = 1
        const val MAX_HOURS: Int = 168
        private val HUNDRED: BigDecimal = BigDecimal(100)

        fun clampHours(requested: Int?): Int = requested?.coerceIn(MIN_HOURS, MAX_HOURS) ?: DEFAULT_HOURS
    }
}
