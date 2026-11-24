package com.sleekydz86.idolglow.servermetric.application.port.out

import com.sleekydz86.idolglow.servermetric.domain.ServerMetricSnapshot
import java.time.LocalDateTime

interface ServerMetricPort {
    fun save(snapshot: ServerMetricSnapshot): ServerMetricSnapshot

    fun findMeasuredSince(from: LocalDateTime): List<ServerMetricSnapshot>

    fun deleteMeasuredAtOrBefore(cutoff: LocalDateTime): Int
}
