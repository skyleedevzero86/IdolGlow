package com.sleekydz86.idolglow.like.infrastructure

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import com.sleekydz86.idolglow.like.application.port.out.LikeTargetPort
import com.sleekydz86.idolglow.like.domain.LikeTarget
import com.sleekydz86.idolglow.like.domain.LikeTargetType
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class LikeTargetAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : LikeTargetPort {
    override fun isLikeable(target: LikeTarget): Boolean =
        when (target.type) {
            LikeTargetType.ARTICLE -> publishedArticleExists(target.key)
            LikeTargetType.EVENT_INFO -> eventExists(target.key)
            LikeTargetType.PRODUCT_REVIEW -> exists(VISIBLE_REVIEW_SQL, target.key.toLongOrNull())
            LikeTargetType.CONTENT_COMMENT -> exists(ACTIVE_COMMENT_SQL, target.key.toLongOrNull())
            LikeTargetType.GLOW_COMMUNITY_POST -> false
        }

    private fun publishedArticleExists(key: String): Boolean {
        val issueSlug = key.substringBefore('/', "")
        val articleSlug = key.substringAfter('/', "")
        if (issueSlug.isEmpty() || articleSlug.isEmpty()) return false
        val params =
            MapSqlParameterSource()
                .addValue("issueSlug", issueSlug)
                .addValue("articleSlug", articleSlug)
                .addValue("today", LocalDate.now(IdolGlowDateTimes.ZONE))
        return count(PUBLISHED_ARTICLE_SQL, params) > 0
    }

    private fun eventExists(key: String): Boolean {
        val source = key.substringBefore(':', "")
        val sourceId = key.substringAfter(':', "")
        if (source.isEmpty() || sourceId.isEmpty()) return false
        val params = MapSqlParameterSource().addValue("source", source).addValue("sourceId", sourceId)
        return count(EVENT_SQL, params) > 0
    }

    private fun exists(
        sql: String,
        id: Long?,
    ): Boolean = id != null && count(sql, MapSqlParameterSource("id", id)) > 0

    private fun count(
        sql: String,
        params: MapSqlParameterSource,
    ): Long = jdbcTemplate.queryForObject(sql, params, Long::class.java) ?: 0L

    companion object {
        private const val PUBLISHED_ARTICLE_SQL =
            "SELECT COUNT(*) FROM webzine_articles a JOIN webzine_issues i ON i.id = a.issue_id " +
                "WHERE i.slug = :issueSlug AND a.slug = :articleSlug AND i.issue_date <= :today"
        private const val EVENT_SQL =
            "SELECT COUNT(*) FROM event_source_ref WHERE source = :source AND source_id = :sourceId"
        private const val VISIBLE_REVIEW_SQL =
            "SELECT COUNT(*) FROM product_reviews WHERE id = :id AND hidden_at IS NULL"
        private const val ACTIVE_COMMENT_SQL =
            "SELECT COUNT(*) FROM content_comments WHERE id = :id AND deleted_at IS NULL"
    }
}
