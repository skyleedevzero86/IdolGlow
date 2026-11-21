package com.sleekydz86.idolglow.eventinfo.infrastructure

import com.sleekydz86.idolglow.eventinfo.application.port.out.CollectedAtRow
import com.sleekydz86.idolglow.eventinfo.application.port.out.CollectedAtTimeZone
import com.sleekydz86.idolglow.eventinfo.application.port.out.EventCollectLogBackfillPort
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Repository
class EventCollectLogBackfillAdapter(
    private val jdbcTemplate: JdbcTemplate,
) : EventCollectLogBackfillPort {
    override fun findMissingTimeZone(
        afterId: Long,
        limit: Int,
    ): List<CollectedAtRow> =
        jdbcTemplate.query(
            "select id, collected_at from event_collect_log " +
                "where collected_at_tz is null and id > ? order by id limit ?",
            { rs, _ -> CollectedAtRow(rs.getLong("id"), rs.getObject("collected_at", LocalDateTime::class.java)) },
            afterId,
            limit,
        )

    @Transactional
    override fun fillTimeZone(rows: List<CollectedAtTimeZone>): Int =
        jdbcTemplate
            .batchUpdate(
                "update event_collect_log set collected_at_tz = ? where id = ? and collected_at_tz is null",
                rows,
                rows.size,
            ) { statement, row ->
                statement.setObject(1, row.collectedAtTz)
                statement.setLong(2, row.id)
            }.sumOf { batch -> batch.sumOf { count -> count.coerceAtLeast(0) } }

    override fun countMissingTimeZone(): Long =
        jdbcTemplate.queryForObject(
            "select count(*) from event_collect_log where collected_at_tz is null",
            Long::class.java,
        ) ?: 0L
}
