package com.sleekydz86.idolglow.glowfeed.infrastructure

import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedKind

internal object GlowFeedSql {
    const val EVENT_LIMIT_IN_ALL = 6

    private const val COLUMNS = "kind, ref_id, sort_at, like_type, like_key, view_type, view_key, base_likes"

    private const val ARTICLE =
        "SELECT 'ARTICLE' AS kind, a.id AS ref_id, a.created_at AS sort_at, 'ARTICLE' AS like_type, " +
            "CONCAT(i.slug, '/', a.slug) AS like_key, 'ARTICLE' AS view_type, " +
            "CONCAT(i.slug, '/', a.slug) AS view_key, 0 AS base_likes " +
            "FROM webzine_articles a JOIN webzine_issues i ON i.id = a.issue_id " +
            "WHERE i.issue_date <= :today"

    private const val EVENT =
        "SELECT 'EVENT' AS kind, r.id AS ref_id, r.created_at AS sort_at, 'EVENT_INFO' AS like_type, " +
            "CONCAT(r.source, ':', r.source_id) AS like_key, 'EVENT_INFO' AS view_type, " +
            "CONCAT(r.source, ':', r.source_id) AS view_key, 0 AS base_likes " +
            "FROM event_source_ref r " +
            "WHERE (r.end_date >= :today OR (r.end_date IS NULL AND r.start_date >= :today)) " +
            "AND (r.global_id IS NULL OR NOT EXISTS (" +
            "SELECT 1 FROM event_source_ref d WHERE d.global_id = r.global_id AND d.id < r.id))"

    private const val PRODUCT_REVIEW =
        "SELECT 'PRODUCT_REVIEW' AS kind, pr.id AS ref_id, pr.created_at AS sort_at, " +
            "'PRODUCT_REVIEW' AS like_type, CONCAT(pr.id, '') AS like_key, 'PRODUCT' AS view_type, " +
            "CONCAT(pr.product_id, '') AS view_key, 0 AS base_likes " +
            "FROM product_reviews pr WHERE pr.hidden_at IS NULL"

    private const val COMMENT =
        "SELECT 'COMMENT' AS kind, c.id AS ref_id, c.created_at AS sort_at, 'CONTENT_COMMENT' AS like_type, " +
            "CONCAT(c.id, '') AS like_key, c.target_type AS view_type, c.target_key AS view_key, 0 AS base_likes " +
            "FROM content_comments c " +
            "WHERE c.deleted_at IS NULL " +
            "AND c.target_type IN ('ARTICLE', 'PRODUCT', 'GLOW_COMMUNITY_POST', 'EVENT_INFO', 'GLOW', 'ARCHIVE') " +
            "AND NOT EXISTS (SELECT 1 FROM glow_community_posts gp " +
            "WHERE c.target_type = 'GLOW_COMMUNITY_POST' AND CONCAT(gp.id, '') = c.target_key " +
            "AND (gp.blinded = TRUE OR gp.deleted_at IS NOT NULL))"

    private const val COMMUNITY_POST =
        "SELECT 'COMMUNITY_POST' AS kind, p.id AS ref_id, p.created_at AS sort_at, " +
            "'GLOW_COMMUNITY_POST' AS like_type, CONCAT(p.id, '') AS like_key, 'GLOW_COMMUNITY_POST' AS view_type, " +
            "CONCAT(p.id, '') AS view_key, p.like_count AS base_likes " +
            "FROM glow_community_posts p WHERE p.deleted_at IS NULL AND p.blinded = FALSE"

    private const val NEWEST_FIRST = "ORDER BY feed.sort_at DESC, feed.kind ASC, feed.ref_id DESC"

    private const val LIKE_COUNTS =
        "LEFT JOIN (SELECT target_type, target_key, COUNT(*) AS cnt FROM content_likes " +
            "GROUP BY target_type, target_key) lk ON lk.target_type = feed.like_type AND lk.target_key = feed.like_key"

    private const val VIEWER_COUNTS =
        "LEFT JOIN (SELECT content_type, content_key, COUNT(*) AS cnt FROM user_content_views " +
            "GROUP BY content_type, content_key) vw ON vw.content_type = feed.view_type AND vw.content_key = feed.view_key"

    fun page(
        kinds: List<GlowFeedKind>,
        capEvents: Boolean,
    ): String = "SELECT feed.* FROM (${union(kinds, capEvents)}) feed $NEWEST_FIRST LIMIT :limit OFFSET :offset"

    fun count(
        kinds: List<GlowFeedKind>,
        capEvents: Boolean,
    ): String = "SELECT COUNT(*) FROM (${union(kinds, capEvents)}) feed"

    fun mostPopular(kinds: List<GlowFeedKind>): String =
        "SELECT feed.* FROM (${union(kinds, capEvents = false)}) feed $LIKE_COUNTS $VIEWER_COUNTS " +
            "ORDER BY (feed.base_likes + COALESCE(lk.cnt, 0) + COALESCE(vw.cnt, 0)) DESC, " +
            "feed.sort_at DESC, feed.ref_id DESC LIMIT 1"

    private fun union(
        kinds: List<GlowFeedKind>,
        capEvents: Boolean,
    ): String = kinds.joinToString(" UNION ALL ") { kind -> branch(kind, capEvents) }

    private fun branch(
        kind: GlowFeedKind,
        capEvents: Boolean,
    ): String =
        when (kind) {
            GlowFeedKind.ARTICLE -> ARTICLE
            GlowFeedKind.EVENT -> if (capEvents) cappedEvents() else EVENT
            GlowFeedKind.PRODUCT_REVIEW -> PRODUCT_REVIEW
            GlowFeedKind.COMMENT -> COMMENT
            GlowFeedKind.COMMUNITY_POST -> COMMUNITY_POST
        }

    private fun cappedEvents(): String =
        "SELECT $COLUMNS FROM ($EVENT ORDER BY sort_at DESC, ref_id DESC LIMIT $EVENT_LIMIT_IN_ALL) recent_events"
}
