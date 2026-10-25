package com.sleekydz86.idolglow.eventinfo.application.port.out

import com.sleekydz86.idolglow.eventinfo.domain.CollectedPerformance
import java.time.LocalDate

interface PerformanceSourceBatchPort {
    val source: String

    fun fetch(
        from: LocalDate,
        to: LocalDate,
    ): List<CollectedPerformance>
}
