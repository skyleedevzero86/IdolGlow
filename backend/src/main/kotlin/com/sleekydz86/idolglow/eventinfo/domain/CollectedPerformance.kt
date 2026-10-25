package com.sleekydz86.idolglow.eventinfo.domain

import java.time.LocalDate

data class CollectedPerformance(
    val source: String,
    val sourceId: String,
    val title: String,
    val venue: String?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val rawPayload: String,
)
