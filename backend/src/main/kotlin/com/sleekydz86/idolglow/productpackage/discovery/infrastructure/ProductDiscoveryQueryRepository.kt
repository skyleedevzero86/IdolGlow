package com.sleekydz86.idolglow.productpackage.discovery.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductTag
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime

@Repository
class ProductDiscoveryQueryRepository(
    private val entityManager: EntityManager,
) {

    fun findPopularProductIds(size: Int): List<Long> =
        entityManager.createNativeQuery(
            """
            select p.id
            from products p
            left join wishes w on w.product_id = p.id
            left join product_reviews r on r.product_id = p.id and r.hidden_at is null
            group by p.id
            order by count(distinct w.id) desc, coalesce(avg(r.rating), 0) desc, count(distinct r.id) desc, p.id desc
            """.trimIndent()
        )
            .setMaxResults(size)
            .resultList
            .map { (it as Number).toLong() }

    fun findRecommendedProductIds(tagNames: List<String>, size: Int): List<Long> {
        if (tagNames.isEmpty()) {
            return emptyList()
        }

        return entityManager.createQuery(
            """
            select p.id
            from Product p
            join p.productTags pt
            where pt.tagName in :tagNames
            group by p.id
            order by count(distinct pt.tagName) desc, p.id desc
            """.trimIndent(),
            java.lang.Long::class.java
        )
            .setParameter("tagNames", tagNames)
            .setMaxResults(size * 5)
            .resultList
            .map { it.toLong() }
    }

    fun findPreferredTags(userId: Long, size: Int): List<String> {
        val wishScores = loadTagScores(
            """
            select pt.tagName, count(pt.id)
            from Wish w
            join w.product p
            join p.productTags pt
            where w.user.id = :userId
            group by pt.tagName
            """.trimIndent(),
            userId
        )
        val reviewScores = loadTagScores(
            """
            select pt.tagName, count(pt.id)
            from ProductReview r
            join r.product p
            join p.productTags pt
            where r.userId = :userId
            group by pt.tagName
            """.trimIndent(),
            userId
        )

        return (wishScores.keys + reviewScores.keys)
            .associateWith { (wishScores[it] ?: 0L) + (reviewScores[it] ?: 0L) }
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Long>> { it.value }.thenBy { it.key })
            .take(size)
            .map { it.key }
    }

    fun findProductsByIds(productIds: List<Long>): List<Product> {
        if (productIds.isEmpty()) {
            return emptyList()
        }

        return entityManager.createQuery(
            """
            select distinct p from Product p
            left join fetch p.productOptions po
            left join fetch po.option o
            left join fetch p.productLocation
            where p.id in :ids
            """.trimIndent(),
            Product::class.java
        )
            .setParameter("ids", productIds)
            .resultList
    }

    
    fun findPersonalizedCandidateProductIds(
        signalTags: List<String>,
        visitStart: LocalDate?,
        visitEnd: LocalDate?,
        placeKeywords: List<String>,
        now: LocalDateTime,
        limit: Int,
    ): List<Long> {
        val acc = LinkedHashSet<Long>()

        if (signalTags.isNotEmpty()) {
            @Suppress("UNCHECKED_CAST")
            val tagIds = entityManager.createQuery(
                """
                select distinct p.id from Product p
                join p.productTags pt
                where pt.tagName in :tags
                """.trimIndent(),
                java.lang.Long::class.java
            )
                .setParameter("tags", signalTags)
                .resultList as List<java.lang.Long>
            acc.addAll(tagIds.map { it.toLong() })
        }

        if (visitStart != null && visitEnd != null) {
            @Suppress("UNCHECKED_CAST")
            val slotIds = entityManager.createQuery(
                """
                select distinct rs.product.id from ReservationSlot rs
                where rs.reservationDate between :start and :end
                and rs.isStatusBooked = false
                and (rs.holdExpiresAt is null or rs.holdExpiresAt < :now)
                """.trimIndent(),
                java.lang.Long::class.java
            )
                .setParameter("start", visitStart)
                .setParameter("end", visitEnd)
                .setParameter("now", now)
                .resultList as List<java.lang.Long>
            acc.addAll(slotIds.map { it.toLong() })
        }

        for (raw in placeKeywords) {
            val kw = sanitizeLikeKeyword(raw)
            if (kw.isEmpty()) continue
            val pattern = "%${kw.lowercase()}%"
            @Suppress("UNCHECKED_CAST")
            val locIds = entityManager.createQuery(
                """
                select distinct pl.product.id from ProductLocation pl
                where lower(concat(concat(coalesce(pl.roadAddressName,''), coalesce(pl.addressName,'')), pl.name)) like :p
                """.trimIndent(),
                java.lang.Long::class.java
            )
                .setParameter("p", pattern)
                .resultList as List<java.lang.Long>
            acc.addAll(locIds.map { it.toLong() })

            @Suppress("UNCHECKED_CAST")
            val optIds = entityManager.createQuery(
                """
                select distinct po.product.id from ProductOption po
                join po.option o
                where lower(o.location) like :p
                """.trimIndent(),
                java.lang.Long::class.java
            )
                .setParameter("p", pattern)
                .resultList as List<java.lang.Long>
            acc.addAll(optIds.map { it.toLong() })
        }

        return acc.sorted().take(limit.coerceAtLeast(1))
    }

    fun countAvailableTripDaysByProduct(
        productIds: List<Long>,
        visitStart: LocalDate,
        visitEnd: LocalDate,
        now: LocalDateTime,
    ): Map<Long, Int> {
        if (productIds.isEmpty()) {
            return emptyMap()
        }
        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createQuery(
            """
            select rs.product.id, count(distinct rs.reservationDate)
            from ReservationSlot rs
            where rs.product.id in :ids
            and rs.reservationDate between :start and :end
            and rs.isStatusBooked = false
            and (rs.holdExpiresAt is null or rs.holdExpiresAt < :now)
            group by rs.product.id
            """.trimIndent()
        )
            .setParameter("ids", productIds)
            .setParameter("start", visitStart)
            .setParameter("end", visitEnd)
            .setParameter("now", now)
            .resultList as List<Array<Any>>

        return rows.associate { row ->
            (row[0] as Number).toLong() to (row[1] as Number).toInt()
        }
    }

    private fun sanitizeLikeKeyword(raw: String): String =
        raw.trim().replace("%", "").replace("_", "").replace("\\", "")

    fun findTagNamesByProductIds(productIds: List<Long>): Map<Long, List<String>> {
        if (productIds.isEmpty()) {
            return emptyMap()
        }

        return entityManager.createQuery(
            "select pt from ProductTag pt where pt.product.id in :ids",
            ProductTag::class.java
        )
            .setParameter("ids", productIds)
            .resultList
            .groupBy { it.product.id }
            .mapValues { (_, values) -> values.map { it.tagName }.distinct() }
    }

    fun findWishCounts(productIds: List<Long>): Map<Long, Long> {
        if (productIds.isEmpty()) {
            return emptyMap()
        }

        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createQuery(
            """
            select w.product.id, count(w.id)
            from Wish w
            where w.product.id in :ids
            group by w.product.id
            """.trimIndent()
        )
            .setParameter("ids", productIds)
            .resultList as List<Array<Any>>

        return rows.associate { row ->
            (row[0] as Number).toLong() to (row[1] as Number).toLong()
        }
    }

    fun findReviewMetrics(productIds: List<Long>): Map<Long, ReviewMetric> {
        if (productIds.isEmpty()) {
            return emptyMap()
        }

        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createQuery(
            """
            select r.product.id, coalesce(avg(r.rating.value), 0), count(r.id)
            from ProductReview r
            where r.product.id in :ids
            and r.hiddenAt is null
            group by r.product.id
            """.trimIndent()
        )
            .setParameter("ids", productIds)
            .resultList as List<Array<Any>>

        return rows.associate { row ->
            (row[0] as Number).toLong() to ReviewMetric(
                averageRating = (row[1] as Number).toDouble(),
                reviewCount = (row[2] as Number).toLong()
            )
        }
    }

    private fun loadTagScores(query: String, userId: Long): Map<String, Long> {
        @Suppress("UNCHECKED_CAST")
        val rows = entityManager.createQuery(query)
            .setParameter("userId", userId)
            .resultList as List<Array<Any>>

        return rows.associate { row ->
            row[0] as String to (row[1] as Number).toLong()
        }
    }

    data class ReviewMetric(
        val averageRating: Double,
        val reviewCount: Long,
    )
}
