package com.sleekydz86.idolglow.glowfeed.infrastructure

import com.sleekydz86.idolglow.comment.application.MarkdownExcerpt
import com.sleekydz86.idolglow.glowfeed.application.dto.GlowFeedContent
import com.sleekydz86.idolglow.glowfeed.application.port.out.GlowFeedContentPort
import com.sleekydz86.idolglow.glowfeed.domain.GlowFeedKind
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.stereotype.Component
import java.sql.ResultSet

@Component
class ProductReviewFeedContentAdapter(
    private val jdbcTemplate: NamedParameterJdbcTemplate,
) : GlowFeedContentPort {
    override val kind: GlowFeedKind = GlowFeedKind.PRODUCT_REVIEW

    override fun load(refIds: Collection<Long>): Map<Long, GlowFeedContent> {
        if (refIds.isEmpty()) return emptyMap()
        val rows = jdbcTemplate.query(REVIEW_SQL, MapSqlParameterSource("ids", refIds)) { rs, _ -> ReviewRow.from(rs) }
        val productIds = rows.map { it.productId }.distinct()
        val reviewImages = firstImages(REVIEW_AGGREGATE, rows.map { it.id })
        val productImages = firstImages(PRODUCT_AGGREGATE, productIds)
        val tags = productTags(productIds)
        return rows.associate { row ->
            row.id to
                GlowFeedContent(
                    title = MarkdownExcerpt.of(row.content, TITLE_LENGTH),
                    imageUrl = reviewImages[row.id] ?: productImages[row.productId],
                    path = GlowFeedPaths.product(row.productId),
                    meta = GlowFeedPaths.joinMeta("★${row.rating}", row.productName, row.nickname),
                    tags = tags[row.productId].orEmpty().take(MAX_TAGS),
                )
        }
    }

    private fun productTags(productIds: List<Long>): Map<Long, List<String>> {
        if (productIds.isEmpty()) return emptyMap()
        return jdbcTemplate
            .query(TAG_SQL, MapSqlParameterSource("ids", productIds)) { rs, _ ->
                rs.getLong("product_id") to rs.getString("tag_name")
            }.groupBy({ it.first }, { it.second })
    }

    private fun firstImages(
        aggregateType: String,
        aggregateIds: Collection<Long>,
    ): Map<Long, String> {
        if (aggregateIds.isEmpty()) return emptyMap()
        val params = MapSqlParameterSource("type", aggregateType).addValue("ids", aggregateIds)
        return jdbcTemplate
            .query(IMAGE_SQL, params) { rs, _ -> rs.getLong("aggregate_id") to rs.getString("url") }
            .groupBy({ it.first }, { it.second })
            .mapValues { (_, urls) -> urls.first() }
    }

    private data class ReviewRow(
        val id: Long,
        val productId: Long,
        val rating: Int,
        val content: String,
        val productName: String?,
        val nickname: String?,
    ) {
        companion object {
            fun from(rs: ResultSet): ReviewRow =
                ReviewRow(
                    id = rs.getLong("id"),
                    productId = rs.getLong("product_id"),
                    rating = rs.getInt("rating"),
                    content = rs.getString("content"),
                    productName = rs.getString("product_name"),
                    nickname = rs.getString("nickname"),
                )
        }
    }

    companion object {
        private const val TITLE_LENGTH = 80
        private const val MAX_TAGS = 2
        private const val REVIEW_AGGREGATE = "PRODUCT_REVIEW"
        private const val PRODUCT_AGGREGATE = "PRODUCT"
        private const val REVIEW_SQL =
            "SELECT pr.id, pr.product_id, pr.rating, pr.content, p.name AS product_name, u.nickname " +
                "FROM product_reviews pr LEFT JOIN products p ON p.id = pr.product_id " +
                "LEFT JOIN users u ON u.id = pr.user_id WHERE pr.id IN (:ids)"
        private const val IMAGE_SQL =
            "SELECT aggregate_id, url FROM images WHERE aggregate_type = :type AND aggregate_id IN (:ids) " +
                "ORDER BY aggregate_id, sort_order, id"
        private const val TAG_SQL =
            "SELECT product_id, tag_name FROM product_tag WHERE product_id IN (:ids) ORDER BY product_id, id"
    }
}
