package com.sleekydz86.idolglow.glowfeed.infrastructure

import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedViewTarget
import com.sleekydz86.idolglow.glowfeed.application.port.out.GlowFeedViewerCountPort
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class GlowFeedViewerCountAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : GlowFeedViewerCountPort {
    override fun countViewers(targets: Collection<GlowFeedViewTarget>): Map<GlowFeedViewTarget, Long> =
        targets
            .groupBy({ it.contentType }, { it.contentKey })
            .flatMap { (type, keys) -> countByType(type, keys.distinct()).toList() }
            .toMap()

    private fun countByType(
        contentType: String,
        contentKeys: List<String>,
    ): Map<GlowFeedViewTarget, Long> {
        val params = MapSqlParameterSource("type", contentType).addValue("keys", contentKeys)
        return jdbcTemplate
            .query(VIEWER_SQL, params) { rs, _ ->
                GlowFeedViewTarget(contentType, rs.getString("content_key")) to rs.getLong("viewers")
            }.toMap()
    }

    companion object {
        private const val VIEWER_SQL =
            "SELECT content_key, COUNT(*) AS viewers FROM user_content_views " +
                "WHERE content_type = :type AND content_key IN (:keys) GROUP BY content_key"
    }
}
