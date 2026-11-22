package com.sleekydz86.idolglow.eventinfo.application.port.out

import java.time.LocalDateTime
import java.time.OffsetDateTime

interface EventCollectLogBackfillPort {
    fun findMissingTimeZone(
        afterId: Long,
        limit: Int,
    ): List<CollectedAtRow>

    fun fillTimeZone(rows: List<CollectedAtTimeZone>): Int

    fun countMissingTimeZone(): Long
}

data class CollectedAtRow(
    val id: Long,
    val collectedAt: LocalDateTime,
)

data class CollectedAtTimeZone(
    val id: Long,
    val collectedAtTz: OffsetDateTime,
)
