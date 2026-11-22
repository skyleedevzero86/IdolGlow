package com.sleekydz86.idolglow.eventinfo.application

import com.sleekydz86.idolglow.eventinfo.application.port.out.CollectedAtTimeZone
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventCollectLogBackfillPort
import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class EventCollectLogTimeZoneBackfillService(
    private val eventCollectLogBackfillPort: EventCollectLogBackfillPort,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun backfill(
        batchSize: Int,
        maxBatches: Int = Int.MAX_VALUE,
    ): TimeZoneBackfillResult {
        require(batchSize in 1..MAX_BATCH_SIZE) { "한 번에 옮길 건수는 1부터 $MAX_BATCH_SIZE 사이여야 합니다. batchSize=$batchSize" }
        var cursor = 0L
        var filled = 0
        var batches = 0
        while (batches < maxBatches) {
            val rows = eventCollectLogBackfillPort.findMissingTimeZone(cursor, batchSize)
            if (rows.isEmpty()) break
            filled += eventCollectLogBackfillPort.fillTimeZone(rows.map { toTimeZone(it.id, it.collectedAt) })
            cursor = rows.last().id
            batches += 1
        }
        val remaining = eventCollectLogBackfillPort.countMissingTimeZone()
        log.info("수집 시각 새 칸 옮기기. filled={} batches={} remaining={}", filled, batches, remaining)
        return TimeZoneBackfillResult(filled, batches, remaining)
    }

    private fun toTimeZone(
        id: Long,
        collectedAt: LocalDateTime,
    ): CollectedAtTimeZone = CollectedAtTimeZone(id, collectedAt.atZone(IdolGlowDateTimes.ZONE).toOffsetDateTime())

    companion object {
        private const val MAX_BATCH_SIZE: Int = 5_000
    }
}

data class TimeZoneBackfillResult(
    val filled: Int,
    val batches: Int,
    val remaining: Long,
)
