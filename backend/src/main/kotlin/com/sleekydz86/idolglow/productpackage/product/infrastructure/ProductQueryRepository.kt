package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductTag
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import kotlin.jvm.java

@Repository
class ProductQueryRepository(
    private val entityManager: EntityManager,
) {

    fun findProductsByNoOffset(
        lastId: Long?,
        size: Int,
        tagName: String?,
    ): List<ProductPagingQueryResponse> {
        val productIds = findProductIds(lastId = lastId, size = size, tagName = tagName)
        if (productIds.isEmpty()) return emptyList()

        val products = entityManager.createQuery(
            """
            select distinct p from Product p
            left join fetch p.productOptions po
            left join fetch po.option o
            where p.id in :ids
            order by p.id desc
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

        return products.map { product ->
            ProductPagingQueryResponse.from(
                product = product,
                tagNames = tagNamesByProductId[product.id].orEmpty()
            )
        }
    }

    fun findProductSpecificById(productId: Long): ProductSpecificResponse? {
        val product = entityManager.find(Product::class.java, productId) ?: return null
        return ProductSpecificResponse.from(product)
    }

    private fun findProductIds(
        lastId: Long?,
        size: Int,
        tagName: String?,
    ): List<Long> {
        return if (tagName == null) {
            entityManager.createQuery(
                """
                select p.id from Product p
                where (:lastId is null or p.id < :lastId)
                order by p.id desc
                """.trimIndent(),
                java.lang.Long::class.java
            ).setParameter("lastId", lastId)
                .setMaxResults(size)
                .resultList
                .map { it.toLong() }
        } else {
            entityManager.createQuery(
                """
                select distinct p.id from Product p
                join p.productTags pt
                where pt.tagName = :tagName
                and (:lastId is null or p.id < :lastId)
                order by p.id desc
                """.trimIndent(),
                java.lang.Long::class.java
            ).setParameter("tagName", tagName)
                .setParameter("lastId", lastId)
                .setMaxResults(size)
                .resultList
                .map { it.toLong() }
        }
    }
}
