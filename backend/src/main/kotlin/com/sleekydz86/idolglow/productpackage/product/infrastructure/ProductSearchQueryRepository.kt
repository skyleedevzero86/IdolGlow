package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSearchCriteria
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.LocalDateTime

@Repository
class ProductSearchQueryRepository(
    private val entityManager: EntityManager,
) {

    fun findOrderedProductIds(criteria: ProductSearchCriteria): List<Long> {
        val sql = when (criteria.sort) {
            ProductSort.NEWEST -> buildNewestSql(criteria)
            ProductSort.POPULARITY -> buildAggregateSortSql(criteria, AggregateSortMode.POPULARITY)
            ProductSort.RATING -> buildAggregateSortSql(criteria, AggregateSortMode.RATING)
            ProductSort.REVIEW_COUNT -> buildAggregateSortSql(criteria, AggregateSortMode.REVIEW_COUNT)
            ProductSort.DISTANCE -> buildDistanceSortSql(criteria)
        }
        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createNativeQuery(sql.sql)
            .also { q -> sql.params.forEach { (k, v) -> q.setParameter(k, v) } }
            .resultList as List<Number>
        return rows.map { it.toLong() }
    }

    private data class BuiltSql(val sql: String, val params: Map<String, Any>)

    private fun buildNewestSql(c: ProductSearchCriteria): BuiltSql {
        val params = linkedMapOf<String, Any>(
            "size" to c.size,
            "lastIdBound" to (c.lastId ?: Long.MAX_VALUE),
            "now" to c.now.asTimestamp(),
            "today" to c.today,
        )
        val join = locationJoinClause(c)
        val filter = filterWhereClause(c, params)
        val geo = geoWhereClause(c, params)
        val sql = """
            SELECT p.id
            FROM products p
            $join
            WHERE 1 = 1
            $filter
            $geo
            AND p.id < :lastIdBound
            ORDER BY p.id DESC
            LIMIT :size
        """.trimIndent()
        return BuiltSql(sql, params)
    }

    private fun buildDistanceSortSql(c: ProductSearchCriteria): BuiltSql {
        val params = linkedMapOf<String, Any>(
            "size" to c.size,
            "offset" to c.offset,
            "now" to c.now.asTimestamp(),
            "today" to c.today,
            "nearLat" to c.nearLatitude!!.toDouble(),
            "nearLng" to c.nearLongitude!!.toDouble(),
        )
        val filter = filterWhereClause(c, params)
        val radiusSql = radiusWhereClause(c, params)
        val sql = """
            SELECT t.pid FROM (
                SELECT p.id AS pid,
                    ${distanceExpr()} AS dm
                FROM products p
                INNER JOIN product_locations pl ON pl.product_id = p.id
                WHERE 1 = 1
                $filter
                $radiusSql
            ) t
            WHERE t.dm IS NOT NULL
            ORDER BY t.dm ASC, t.pid DESC
            LIMIT :size OFFSET :offset
        """.trimIndent()
        return BuiltSql(sql, params)
    }

    private enum class AggregateSortMode {
        POPULARITY,
        RATING,
        REVIEW_COUNT,
    }

    private fun buildAggregateSortSql(c: ProductSearchCriteria, mode: AggregateSortMode): BuiltSql {
        val params = linkedMapOf<String, Any>(
            "size" to c.size,
            "offset" to c.offset,
            "now" to c.now.asTimestamp(),
            "today" to c.today,
        )
        val join = locationJoinClause(c)
        val filter = filterWhereClause(c, params)
        val geo = geoWhereClause(c, params)
        val orderBy = when (mode) {
            AggregateSortMode.POPULARITY ->
                "t.wc DESC, t.ar DESC, t.rc DESC, t.pid DESC"
            AggregateSortMode.RATING ->
                "t.ar DESC, t.rc DESC, t.wc DESC, t.pid DESC"
            AggregateSortMode.REVIEW_COUNT ->
                "t.rc DESC, t.ar DESC, t.wc DESC, t.pid DESC"
        }
        val sql = """
            SELECT t.pid FROM (
                SELECT p.id AS pid,
                    (SELECT COUNT(*) FROM wishes w WHERE w.product_id = p.id) AS wc,
                    COALESCE((
                        SELECT AVG(CAST(pr.rating AS DOUBLE)) FROM product_reviews pr WHERE pr.product_id = p.id AND pr.hidden_at IS NULL
                    ), 0) AS ar,
                    COALESCE((
                        SELECT COUNT(*) FROM product_reviews pr2 WHERE pr2.product_id = p.id AND pr2.hidden_at IS NULL
                    ), 0) AS rc
                FROM products p
                $join
                WHERE 1 = 1
                $filter
                $geo
            ) t
            ORDER BY $orderBy
            LIMIT :size OFFSET :offset
        """.trimIndent()
        return BuiltSql(sql, params)
    }

    private fun locationJoinClause(c: ProductSearchCriteria): String {
        if (!c.needsLocationJoin) return ""
        val inner = c.sort == ProductSort.DISTANCE || c.applyRadiusFilter
        return if (inner) {
            " INNER JOIN product_locations pl ON pl.product_id = p.id "
        } else {
            " LEFT JOIN product_locations pl ON pl.product_id = p.id "
        }
    }

    private fun geoWhereClause(c: ProductSearchCriteria, params: MutableMap<String, Any>): String {
        if (!c.needsLocationJoin) return ""
        if (!c.applyRadiusFilter) return ""
        params["nearLat"] = c.nearLatitude!!.toDouble()
        params["nearLng"] = c.nearLongitude!!.toDouble()
        params["radiusMeters"] = c.radiusMeters!!.toDouble()
        return " AND (${distanceExpr()}) <= :radiusMeters "
    }

    private fun radiusWhereClause(c: ProductSearchCriteria, params: MutableMap<String, Any>): String {
        if (!c.applyRadiusFilter) return ""
        params["radiusMeters"] = c.radiusMeters!!.toDouble()
        return " AND (${distanceExpr()}) <= :radiusMeters "
    }

    private fun filterWhereClause(c: ProductSearchCriteria, params: MutableMap<String, Any>): String {
        val sb = StringBuilder()
        val kw = c.keyword?.trim()?.takeIf { it.isNotEmpty() }
        if (kw != null) {
            val pattern = "%${escapeLike(kw.lowercase())}%"
            params["keywordLike"] = pattern
            sb.append(
                """
                AND (
                  LOWER(p.name) LIKE :keywordLike ESCAPE '${LIKE_ESCAPE_CHAR}'
                  OR LOWER(p.description) LIKE :keywordLike ESCAPE '${LIKE_ESCAPE_CHAR}'
                  OR EXISTS (
                    SELECT 1 FROM product_tag ptk
                    WHERE ptk.product_id = p.id
                    AND LOWER(ptk.tag_name) LIKE :keywordLike ESCAPE '${LIKE_ESCAPE_CHAR}'
                  )
                )
                """.trimIndent()
            )
        }
        c.minPrice?.let { mp ->
            params["minPrice"] = mp
            sb.append(
                """
                AND COALESCE((
                  SELECT MIN(o.price) FROM product_option po INNER JOIN options o ON o.id = po.option_id
                  WHERE po.product_id = p.id
                ), 0) >= :minPrice
                """.trimIndent()
            )
        }
        c.maxPrice?.let { xp ->
            params["maxPrice"] = xp
            sb.append(
                """
                AND COALESCE((
                  SELECT MIN(o.price) FROM product_option po INNER JOIN options o ON o.id = po.option_id
                  WHERE po.product_id = p.id
                ), 0) <= :maxPrice
                """.trimIndent()
            )
        }
        val tags = c.effectiveTagNames
        if (tags.isNotEmpty()) {
            params["tagNames"] = tags
            params["expectedTagCount"] = tags.size
            sb.append(
                """
                AND (
                  SELECT COUNT(DISTINCT ptx.tag_name)
                  FROM product_tag ptx
                  WHERE ptx.product_id = p.id AND ptx.tag_name IN (:tagNames)
                ) = :expectedTagCount
                """.trimIndent()
            )
        }
        c.visitDate?.let { d ->
            params["visitDate"] = d
            sb.append(
                """
                AND EXISTS (
                  SELECT 1 FROM reservation_slots rs
                  WHERE rs.product_id = p.id
                  AND rs.reservation_date = :visitDate
                  AND rs.is_booked = FALSE
                  AND (rs.hold_expires_at IS NULL OR rs.hold_expires_at < :now)
                )
                """.trimIndent()
            )
        }
        if (c.reservableOnly && c.visitDate == null) {
            sb.append(
                """
                AND EXISTS (
                  SELECT 1 FROM reservation_slots rs2
                  WHERE rs2.product_id = p.id
                  AND rs2.reservation_date >= :today
                  AND rs2.is_booked = FALSE
                  AND (rs2.hold_expires_at IS NULL OR rs2.hold_expires_at < :now)
                )
                """.trimIndent()
            )
        }
        return sb.toString()
    }

    private fun distanceExpr(): String =
        """
        (6371000 * ACOS(GREATEST(-1.0, LEAST(1.0,
          COS(RADIANS(:nearLat)) * COS(RADIANS(pl.latitude)) * COS(RADIANS(pl.longitude) - RADIANS(:nearLng))
          + SIN(RADIANS(:nearLat)) * SIN(RADIANS(pl.latitude))
        ))))
        """.trimIndent()

    private fun LocalDateTime.asTimestamp(): Timestamp = Timestamp.valueOf(this)

    private fun escapeLike(raw: String): String =
        raw.replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")

    companion object {
        private const val LIKE_ESCAPE_CHAR = '\\'
    }
}
