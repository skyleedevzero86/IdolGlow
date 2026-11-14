package com.sleekydz86.idolglow.glowfeed.infrastructure

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedEntry
import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedViewTarget
import com.sleekydz86.idolglow.glowfeed.application.port.out.GlowFeedIndexPort
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedCategory
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedKind
import org.springframework.jdbc.core.RowMapper
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class GlowFeedIndexAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : GlowFeedIndexPort {
    override fun findPage(
        category: GlowFeedCategory?,
        request: OffsetPageRequest,
        today: LocalDate,
    ): OffsetSlice<GlowFeedEntry> {
        val kinds = GlowFeedKind.of(category)
        val capEvents = category == null
        val params =
            MapSqlParameterSource()
                .addValue("today", today)
                .addValue("limit", request.size)
                .addValue("offset", request.page.toLong() * request.size)
        val total = jdbcTemplate.queryForObject(GlowFeedSql.count(kinds, capEvents), params, Long::class.java) ?: 0L
        if (total == 0L) return OffsetSlice(emptyList(), 0L)
        return OffsetSlice(jdbcTemplate.query(GlowFeedSql.page(kinds, capEvents), params, ENTRY_MAPPER), total)
    }

    override fun findLatest(
        category: GlowFeedCategory,
        today: LocalDate,
    ): GlowFeedEntry? {
        val params =
            MapSqlParameterSource()
                .addValue("today", today)
                .addValue("limit", 1)
                .addValue("offset", 0)
        return jdbcTemplate
            .query(GlowFeedSql.page(GlowFeedKind.of(category), capEvents = false), params, ENTRY_MAPPER)
            .firstOrNull()
    }

    override fun findMostPopular(
        category: GlowFeedCategory,
        today: LocalDate,
    ): GlowFeedEntry? =
        jdbcTemplate
            .query(
                GlowFeedSql.mostPopular(GlowFeedKind.of(category)),
                MapSqlParameterSource("today", today),
                ENTRY_MAPPER,
            ).firstOrNull()

    companion object {
        private val ENTRY_MAPPER =
            RowMapper { rs, _ ->
                GlowFeedEntry(
                    kind = GlowFeedKind.valueOf(rs.getString("kind").trim()),
                    refId = rs.getLong("ref_id"),
                    sortAt = rs.getTimestamp("sort_at").toLocalDateTime(),
                    likeKey = rs.getString("like_key"),
                    viewTarget = GlowFeedViewTarget(rs.getString("view_type"), rs.getString("view_key")),
                )
            }
    }
}
