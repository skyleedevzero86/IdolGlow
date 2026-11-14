package com.sleekydz86.idolglow.glowfeed.infrastructure

import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedContent
import com.sleekydz86.idolglow.glowfeed.application.port.out.GlowFeedContentPort
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedKind
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class EventFeedContentAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : GlowFeedContentPort {
    override val kind: GlowFeedKind = GlowFeedKind.EVENT

    override fun load(refIds: Collection<Long>): Map<Long, GlowFeedContent> {
        if (refIds.isEmpty()) return emptyMap()
        return jdbcTemplate
            .query(EVENT_SQL, MapSqlParameterSource("ids", refIds)) { rs, _ ->
                val period =
                    GlowFeedPaths.dateRange(
                        rs.getDate("start_date")?.toLocalDate(),
                        rs.getDate("end_date")?.toLocalDate(),
                    )
                rs.getLong("id") to
                    GlowFeedContent(
                        title = rs.getString("title"),
                        imageUrl = null,
                        path = GlowFeedPaths.eventInfo(rs.getString("source"), rs.getString("source_id")),
                        meta = GlowFeedPaths.joinMeta(period, rs.getString("venue")),
                    )
            }.toMap()
    }

    companion object {
        private const val EVENT_SQL =
            "SELECT id, source, source_id, title, start_date, end_date, venue FROM event_source_ref WHERE id IN (:ids)"
    }
}
