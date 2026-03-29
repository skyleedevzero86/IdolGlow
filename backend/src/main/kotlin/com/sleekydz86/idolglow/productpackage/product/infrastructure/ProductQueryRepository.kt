package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductTag
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductLocationSummaryResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse
import jakarta.persistence.EntityManager
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class ProductQueryRepository(
    private val entityManager: EntityManager,
) {

    fun hydrateForBrowse(
        orderedIds: List<Long>,
        wishCounts: Map<Long, Long>,
        reviewMetrics: Map<Long, ReviewMetricRow>,
        nearLatitude: BigDecimal?,
        nearLongitude: BigDecimal?,
    ): List<ProductPagingQueryResponse> {
        if (orderedIds.isEmpty()) return emptyList()

        @Suppress("UNCHECKED_CAST")
        val products = entityManager.createQuery(
            """
            select distinct p from Product p
            left join fetch p.productOptions po
            left join fetch po.option o
            left join fetch p.productLocation
            where p.id in :ids
            """.trimIndent(),
            Product::class.java
        ).setParameter("ids", orderedIds).resultList as List<Product>

        val tags = entityManager.createQuery(
            "select pt from ProductTag pt where pt.product.id in :ids",
            ProductTag::class.java
        ).setParameter("ids", orderedIds).resultList

        val tagNamesByProductId: Map<Long, List<String>> =
            tags.groupBy { it.product.id }
                .mapValues { (_, values) -> values.map { it.tagName }.distinct() }

        val productById = products.associateBy { it.id }
        return orderedIds.mapNotNull { id ->
            val product = productById[id] ?: return@mapNotNull null
            val loc = product.productLocation
            val distanceMeters =
                if (nearLatitude != null && nearLongitude != null && loc != null) {
                    GeoDistanceMeters.between(
                        nearLatitude,
                        nearLongitude,
                        loc.latitude,
                        loc.longitude,
                    )
                } else {
                    null
                }
            val locDto = loc?.let { ProductLocationSummaryResponse.from(it) }
            val metric = reviewMetrics[id]
            ProductPagingQueryResponse.from(
                product = product,
                tagNames = tagNamesByProductId[product.id].orEmpty(),
                location = locDto,
                distanceMeters = distanceMeters,
                wishCount = wishCounts[id] ?: 0L,
                averageRating = metric?.averageRating ?: 0.0,
                reviewCount = metric?.reviewCount ?: 0L,
            )
        }
    }

    fun findProductSpecificById(productId: Long): ProductSpecificResponse? {
        val product = entityManager.find(Product::class.java, productId) ?: return null
        return ProductSpecificResponse.from(product)
    }

    data class ReviewMetricRow(
        val averageRating: Double,
        val reviewCount: Long,
    )
}
