package com.sleekydz86.idolglow.idolglowissue.infrastructure

import com.sleekydz86.idolglow.global.support.OffsetPageRequest
import com.sleekydz86.idolglow.global.support.OffsetSlice
import com.sleekydz86.idolglow.idolglowissue.application.IdolGlowArticleRankingPort
import com.sleekydz86.idolglow.idolglowissue.application.dto.RankedIdolGlowArticle
import com.sleekydz86.idolglow.idolglowissue.domain.IdolGlowArticleSort
import com.sleekydz86.idolglow.idolglowissue.domain.IssueCategory
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet
import java.time.LocalDate

@Component
class IdolGlowArticleRankingJdbcAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : IdolGlowArticleRankingPort {
    override fun findPublished(
        sort: IdolGlowArticleSort,
        request: OffsetPageRequest,
        today: LocalDate,
    ): OffsetSlice<RankedIdolGlowArticle> {
        val params =
            MapSqlParameterSource()
                .addValue("today", today)
                .addValue("limit", request.size)
                .addValue("offset", request.page.toLong() * request.size)
        val total = jdbcTemplate.queryForObject(COUNT_SQL, params, Long::class.java) ?: 0L
        if (total == 0L) return OffsetSlice(emptyList(), 0L)
        val rows =
            jdbcTemplate.query("$PAGE_SQL ${orderBy(sort)} LIMIT :limit OFFSET :offset", params) { rs, _ ->
                rs.toRanked()
            }
        val tags = findTags(rows.map { it.articleId })
        return OffsetSlice(rows.map { it.copy(tags = tags[it.articleId].orEmpty()) }, total)
    }

    private fun findTags(articleIds: List<Long>): Map<Long, List<String>> {
        if (articleIds.isEmpty()) return emptyMap()
        return jdbcTemplate
            .query(TAG_SQL, MapSqlParameterSource("ids", articleIds)) { rs, _ ->
                rs.getLong("article_id") to rs.getString("tag_name")
            }.groupBy({ it.first }, { it.second })
    }

    private fun orderBy(sort: IdolGlowArticleSort): String =
        when (sort) {
            IdolGlowArticleSort.LATEST -> "ORDER BY $LATEST_ORDER"
            IdolGlowArticleSort.POPULAR -> "ORDER BY (like_count + viewer_count) DESC, $LATEST_ORDER"
            IdolGlowArticleSort.LIKES -> "ORDER BY like_count DESC, $LATEST_ORDER"
        }

    companion object {
        private const val LATEST_ORDER = "issue_date DESC, volume DESC, created_at DESC, id DESC"
        private const val COUNT_SQL =
            "SELECT COUNT(*) FROM idolglow_articles a JOIN idolglow_issues i ON i.id = a.issue_id " +
                "WHERE i.issue_date <= :today"
        private const val PAGE_SQL =
            "SELECT * FROM (SELECT a.id, a.slug, a.title, a.summary, a.card_image_url, a.category, a.created_at, " +
                "i.slug AS issue_slug, i.volume, i.issue_date, " +
                "COALESCE(lk.cnt, 0) AS like_count, COALESCE(vw.cnt, 0) AS viewer_count " +
                "FROM idolglow_articles a JOIN idolglow_issues i ON i.id = a.issue_id " +
                "LEFT JOIN (SELECT target_key, COUNT(*) AS cnt FROM content_likes WHERE target_type = 'ARTICLE' " +
                "GROUP BY target_key) lk ON lk.target_key = CONCAT(i.slug, '/', a.slug) " +
                "LEFT JOIN (SELECT content_key, COUNT(*) AS cnt FROM user_content_views " +
                "WHERE content_type = 'ARTICLE' GROUP BY content_key) vw " +
                "ON vw.content_key = CONCAT(i.slug, '/', a.slug) " +
                "WHERE i.issue_date <= :today) ranked"
        private const val TAG_SQL =
            "SELECT article_id, tag_name FROM idolglow_article_tags WHERE article_id IN (:ids) " +
                "ORDER BY article_id, display_order"

        private fun ResultSet.toRanked(): RankedIdolGlowArticle =
            RankedIdolGlowArticle(
                articleId = getLong("id"),
                articleSlug = getString("slug"),
                title = getString("title"),
                summary = getString("summary"),
                cardImageUrl = getString("card_image_url"),
                category = IssueCategory.valueOf(getString("category")),
                issueSlug = getString("issue_slug"),
                volume = getInt("volume"),
                issueDate = getDate("issue_date").toLocalDate(),
                tags = emptyList(),
                likeCount = getLong("like_count"),
                viewerCount = getLong("viewer_count"),
            )
    }
}
