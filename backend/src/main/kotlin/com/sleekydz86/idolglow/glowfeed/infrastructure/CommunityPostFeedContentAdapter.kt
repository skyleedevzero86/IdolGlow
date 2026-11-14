package com.sleekydz86.idolglow.glowfeed.infrastructure

import com.sleekydz86.idolglow.glowcommunity.domain.GlowCommunityTag
import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedContent
import com.sleekydz86.idolglow.glowfeed.application.port.out.GlowFeedContentPort
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedKind
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component

@Component
class CommunityPostFeedContentAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : GlowFeedContentPort {
    override val kind: GlowFeedKind = GlowFeedKind.COMMUNITY_POST

    override fun load(refIds: Collection<Long>): Map<Long, GlowFeedContent> {
        if (refIds.isEmpty()) return emptyMap()
        return jdbcTemplate
            .query(POST_SQL, MapSqlParameterSource("ids", refIds)) { rs, _ ->
                val id = rs.getLong("id")
                val tagLabel = GlowCommunityTag.entries.firstOrNull { it.name == rs.getString("tag") }?.label
                id to
                    GlowFeedContent(
                        title = rs.getString("title"),
                        imageUrl = GlowFeedPaths.firstMarkdownImage(rs.getString("content_markdown")),
                        path = GlowFeedPaths.communityPost(id),
                        meta = GlowFeedPaths.joinMeta(rs.getString("nickname"), "댓글 ${rs.getLong("comment_count")}"),
                        tags = listOfNotNull(tagLabel),
                    )
            }.toMap()
    }

    companion object {
        private const val POST_SQL =
            "SELECT p.id, p.tag, p.title, p.content_markdown, p.comment_count, u.nickname " +
                "FROM glow_community_posts p LEFT JOIN users u ON u.id = p.user_id WHERE p.id IN (:ids)"
    }
}
