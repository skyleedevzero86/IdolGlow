package com.sleekydz86.idolglow.servermetric.application

import com.sleekydz86.idolglow.admin.application.InfrastructureProbe
import com.sleekydz86.idolglow.admin.application.dto.AdminInfrastructureStatusResult
import com.sleekydz86.idolglow.global.support.DashboardTime
import com.sleekydz86.idolglow.servermetric.application.port.out.ServerMetricPort
import com.sleekydz86.idolglow.servermetric.domain.ServerMetricSnapshot
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class ServerMetricRecorder(
    private val infrastructureProbe: InfrastructureProbe,
    private val httpResponseCounter: HttpResponseCounter,
    private val serverMetricPort: ServerMetricPort,
    private val dashboardTime: DashboardTime,
) {
    fun record(): ServerMetricSnapshot {
        val infrastructure = infrastructureProbe.probeAll()
        val responses = httpResponseCounter.takeSinceLast()
        val database = infrastructure.byType(DATABASE)
        val minio = infrastructure.byType(MINIO)
        val snapshot =
            ServerMetricSnapshot(
                measuredAt = dashboardTime.now(),
                overallStatus = infrastructureProbe.overallStatus(infrastructure),
                dbStatus = database.status,
                dbResponseMs = database.responseTimeMs.takeIf { database.status == ServerMetricSnapshot.STATUS_UP },
                minioStatus = minio.status,
                minioResponseMs = minio.responseTimeMs.takeIf { minio.status == ServerMetricSnapshot.STATUS_UP },
                httpRequestCount = responses.requests,
                httpErrorCount = responses.errors,
            )
        return save(snapshot)
    }

    @Transactional
    fun purgeOld(): Int {
        val deleted = serverMetricPort.deleteMeasuredAtOrBefore(dashboardTime.hoursAgo(KEEP_HOURS))
        log.info("서버 점검 기록 보관 정리 deleted={}", deleted)
        return deleted
    }

    private fun save(snapshot: ServerMetricSnapshot): ServerMetricSnapshot {
        val saved = serverMetricPort.save(snapshot)
        log.info(
            "서버 점검 기록 저장 overall={} db={} minio={} requests={} errors={}",
            saved.overallStatus,
            saved.dbStatus,
            saved.minioStatus,
            saved.httpRequestCount,
            saved.httpErrorCount,
        )
        return saved
    }

    private fun List<AdminInfrastructureStatusResult>.byType(type: String): AdminInfrastructureStatusResult =
        firstOrNull { it.type == type }
            ?: AdminInfrastructureStatusResult(
                type = type,
                label = type,
                status = ServerMetricSnapshot.STATUS_DOWN,
                message = "점검 결과가 없습니다.",
                responseTimeMs = null,
                details = emptyMap(),
            )

    companion object {
        private val log = LoggerFactory.getLogger(ServerMetricRecorder::class.java)
        const val KEEP_HOURS: Long = 168L
        private const val DATABASE: String = "DATABASE"
        private const val MINIO: String = "MINIO"
    }
}
