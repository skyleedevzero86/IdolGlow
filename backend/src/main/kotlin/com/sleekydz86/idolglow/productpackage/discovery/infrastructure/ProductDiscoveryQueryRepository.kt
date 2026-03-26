package com.sleekydz86.idolglow.productpackage.discovery.infrastructure

import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository

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
            left join product_reviews r on r.product_id = p.id
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
            where p.id in :ids
            """.trimIndent(),
            Product::class.java
        )
            .setParameter("ids", productIds)
            .resultList
    }

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
