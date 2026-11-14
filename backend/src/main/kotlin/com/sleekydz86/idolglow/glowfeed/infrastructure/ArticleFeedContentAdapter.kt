package com.sleekydz86.idolglow.glowfeed.infrastructure

import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedContent
import com.sleekydz86.idolglow.glowfeed.application.port.out.GlowFeedContentPort
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedKind
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class ArticleFeedContentAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : GlowFeedContentPort {
    override val kind: GlowFeedKind = GlowFeedKind.ARTICLE

    override fun load(refIds: Collection<Long>): Map<Long, GlowFeedContent> {
        if (refIds.isEmpty()) return emptyMap()
        val params = MapSqlParameterSource("ids", refIds)
        val tags =
            jdbcTemplate
                .query(TAG_SQL, params) { rs, _ -> rs.getLong("article_id") to rs.getString("tag_name") }
                .groupBy({ it.first }, { it.second })
        return jdbcTemplate
            .query(ARTICLE_SQL, params) { rs, _ ->
                val id = rs.getLong("id")
                id to
                    GlowFeedContent(
                        title = rs.getString("title"),
                        imageUrl = rs.getString("card_image_url")?.takeIf(String::isNotBlank),
                        path = GlowFeedPaths.webzineArticle(rs.getString("issue_slug"), rs.getString("slug")),
                        volume = rs.getInt("volume"),
                        meta = rs.getString("author_name"),
                        tags = tags[id].orEmpty(),
                    )
            }.toMap()
    }

    companion object {
        private const val ARTICLE_SQL =
            "SELECT a.id, a.slug, a.title, a.card_image_url, a.author_name, i.slug AS issue_slug, i.volume " +
                "FROM webzine_articles a JOIN webzine_issues i ON i.id = a.issue_id WHERE a.id IN (:ids)"
        private const val TAG_SQL =
            "SELECT article_id, tag_name FROM webzine_article_tags WHERE article_id IN (:ids) " +
                "ORDER BY article_id, display_order"
    }
}
