package com.sleekydz86.idolglow.glowfeed.infrastructure

import com.sleekydz86.idolglow.comment.application.MarkdownExcerpt
import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedContent
import com.sleekydz86.idolglow.glowfeed.application.port.out.GlowFeedContentPort
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedKind
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class CommentFeedContentAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : GlowFeedContentPort {
    override val kind: GlowFeedKind = GlowFeedKind.COMMENT

    override fun load(refIds: Collection<Long>): Map<Long, GlowFeedContent> {
        if (refIds.isEmpty()) return emptyMap()
        return jdbcTemplate
            .query(COMMENT_SQL, MapSqlParameterSource("ids", refIds)) { rs, _ ->
                val markdown = rs.getString("content_markdown")
                val path =
                    GlowFeedPaths.commentTarget(
                        rs.getString("target_type"),
                        rs.getString("target_key"),
                        rs.getString("target_path"),
                    ) ?: return@query null
                rs.getLong("id") to
                    GlowFeedContent(
                        title = MarkdownExcerpt.of(markdown, TITLE_LENGTH),
                        imageUrl = GlowFeedPaths.firstMarkdownImage(markdown),
                        path = path,
                        meta = GlowFeedPaths.joinMeta(rs.getString("target_title"), rs.getString("nickname")),
                    )
            }.filterNotNull()
            .toMap()
    }

    companion object {
        private const val TITLE_LENGTH = 80
        private const val COMMENT_SQL =
            "SELECT c.id, c.target_type, c.target_key, c.target_title, c.target_path, c.content_markdown, " +
                "u.nickname FROM content_comments c LEFT JOIN users u ON u.id = c.user_id WHERE c.id IN (:ids)"
    }
}
