package com.sleekydz86.idolglow.servermetric.domain

import com.sleekydz86.idolglow.global.infrastructure.persistence.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "server_metric_snapshots")
class ServerMetricSnapshot(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L,
    @Column(name = "measured_at", nullable = false, updatable = false)
    val measuredAt: LocalDateTime,
    @Column(name = "overall_status", nullable = false, updatable = false, length = STATUS_LENGTH)
    val overallStatus: String,
    @Column(name = "db_status", nullable = false, updatable = false, length = STATUS_LENGTH)
    val dbStatus: String,
    @Column(name = "db_response_ms", updatable = false)
    val dbResponseMs: Long?,
    @Column(name = "minio_status", nullable = false, updatable = false, length = STATUS_LENGTH)
    val minioStatus: String,
    @Column(name = "minio_response_ms", updatable = false)
    val minioResponseMs: Long?,
    @Column(name = "http_request_count", nullable = false, updatable = false)
    val httpRequestCount: Long,
    @Column(name = "http_error_count", nullable = false, updatable = false)
    val httpErrorCount: Long,
) : BaseEntity() {
    fun isUp(): Boolean = overallStatus == STATUS_UP

    companion object {
        const val STATUS_LENGTH: Int = 20
        const val STATUS_UP: String = "UP"
        const val STATUS_DOWN: String = "DOWN"
    }
}
