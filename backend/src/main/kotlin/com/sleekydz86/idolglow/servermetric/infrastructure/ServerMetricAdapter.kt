package com.sleekydz86.idolglow.servermetric.infrastructure

import com.sleekydz86.idolglow.servermetric.application.port.out.ServerMetricPort
import com.sleekydz86.idolglow.servermetric.domain.ServerMetricSnapshot
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Component
import java.time.LocalDateTime

interface ServerMetricSnapshotJpaRepository : JpaRepository<ServerMetricSnapshot, Long> {
    fun findByMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(from: LocalDateTime): List<ServerMetricSnapshot>

    @Modifying
    @Query("DELETE FROM ServerMetricSnapshot s WHERE s.measuredAt <= :cutoff")
    fun deleteMeasuredAtOrBefore(
        @Param("cutoff") cutoff: LocalDateTime,
    ): Int
}

@Component
class ServerMetricAdapter(
    private val repository: ServerMetricSnapshotJpaRepository,
) : ServerMetricPort {
    override fun save(snapshot: ServerMetricSnapshot): ServerMetricSnapshot = repository.save(snapshot)

    override fun findMeasuredSince(from: LocalDateTime): List<ServerMetricSnapshot> =
        repository.findByMeasuredAtGreaterThanEqualOrderByMeasuredAtAsc(from)

    override fun deleteMeasuredAtOrBefore(cutoff: LocalDateTime): Int = repository.deleteMeasuredAtOrBefore(cutoff)
}
