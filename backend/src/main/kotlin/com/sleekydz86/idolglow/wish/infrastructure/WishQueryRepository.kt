package com.sleekydz86.idolglow.wish.infrastructure

import com.sleekydz86.idolglow.wish.application.dto.WishedProductPagingResponse
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class WishQueryRepository(
    private val entityManager: EntityManager
) {

    fun findByWishedProductsByNoOffset(
        userId: Long,
        lastWishId: Long?,
        size: Int
    ): List<WishedProductPagingResponse> {
        val productIds = findWishedProductIds(userId = userId, lastWishId = lastWishId, size = size)
        if (productIds.isEmpty()) return emptyList()

        val products = entityManager.createQuery(
            """
            select distinct p from Product p
            where p.id in :ids
            """.trimIndent(),
            Product::class.java
        ).setParameter("ids", productIds).resultList

        val tags = entityManager.createQuery(
            "select pt from ProductTag pt where pt.product.id in :ids",
            ProductTag::class.java
        ).setParameter("ids", productIds).resultList

        val tagNamesByProductId: Map<Long, List<String>> =
            tags.groupBy { it.product.id }
                .mapValues { (_, values) -> values.map { it.tagName }.distinct() }

        val productById = products.associateBy { it.id }

        return productIds.mapNotNull { productId ->
            val product = productById[productId] ?: return@mapNotNull null
            WishedProductPagingResponse.from(
                product = product,
                tagNames = tagNamesByProductId[product.id].orEmpty()
            )
        }
    }

    private fun findWishedProductIds(
        userId: Long,
        lastWishId: Long?,
        size: Int
    ): List<Long> {
        val jpql = buildString {
            append(
                """
            select w.product.id
            from Wish w
            where w.user.id = :userId
            """.trimIndent()
            )
            if (lastWishId != null) {
                append("\n  and w.id < :lastWishId")
            }
            append("\norder by w.id desc")
        }

        val query = entityManager.createQuery(jpql, java.lang.Long::class.java)
            .setParameter("userId", userId)
            .setMaxResults(size)

        if (lastWishId != null) {
            query.setParameter("lastWishId", lastWishId)
        }

        return query.resultList.map { it.toLong() }
    }

}
