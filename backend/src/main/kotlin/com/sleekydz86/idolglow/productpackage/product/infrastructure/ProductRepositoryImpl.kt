package com.sleekydz86.idolglow.productpackage.product.infrastructure

import com.sleekydz86.idolglow.productpackage.discovery.infrastructure.ProductDiscoveryQueryRepository
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.ProductRepository
import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductBrowseResult
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSearchCriteria
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val productCommandRepository: ProductCommandRepository,
    private val productQueryRepository: ProductQueryRepository,
    private val productSearchQueryRepository: ProductSearchQueryRepository,
    private val productDiscoveryQueryRepository: ProductDiscoveryQueryRepository,
) : ProductRepository {

    override fun findProductsByNoOffset(
        lastId: Long?,
        size: Int,
        tagName: String?,
    ): List<ProductPagingQueryResponse> =
        browseProducts(
            ProductSearchCriteria(
                lastId = lastId,
                offset = 0,
                size = size,
                tag = tagName,
                tags = emptyList(),
                keyword = null,
                minPrice = null,
                maxPrice = null,
                visitDate = null,
                reservableOnly = false,
                sort = ProductSort.NEWEST,
                nearLatitude = null,
                nearLongitude = null,
                radiusMeters = null,
                now = java.time.LocalDateTime.now(java.time.ZoneOffset.UTC),
                today = java.time.LocalDate.now(java.time.ZoneOffset.UTC),
            )
        ).items

    override fun browseProducts(criteria: ProductSearchCriteria): ProductBrowseResult {
        val ids = productSearchQueryRepository.findOrderedProductIds(criteria)
        if (ids.isEmpty()) {
            return ProductBrowseResult(items = emptyList(), nextCursor = null, nextOffset = null)
        }
        val wishCounts = productDiscoveryQueryRepository.findWishCounts(ids)
        val reviewMetrics = productDiscoveryQueryRepository.findReviewMetrics(ids)
        val reviewRows = reviewMetrics.mapValues { (_, v) ->
            ProductQueryRepository.ReviewMetricRow(v.averageRating, v.reviewCount)
        }
        val items = productQueryRepository.hydrateForBrowse(
            orderedIds = ids,
            wishCounts = wishCounts,
            reviewMetrics = reviewRows,
            nearLatitude = criteria.nearLatitude,
            nearLongitude = criteria.nearLongitude,
        )
        val nextCursor =
            if (ids.size == criteria.size && criteria.sort == ProductSort.NEWEST) ids.last() else null
        val nextOffset =
            if (ids.size == criteria.size && criteria.sort != ProductSort.NEWEST) {
                criteria.offset + criteria.size
            } else {
                null
            }
        return ProductBrowseResult(items = items, nextCursor = nextCursor, nextOffset = nextOffset)
    }

    override fun findProductSpecificById(productId: Long): ProductSpecificResponse? =
        productQueryRepository.findProductSpecificById(productId)

    override fun findById(productId: Long): Product? {
        return productCommandRepository.findById(productId)
    }

    override fun existsById(productId: Long): Boolean =
        productCommandRepository.existsById(productId)

    override fun getReferenceById(productId: Long): Product =
        productCommandRepository.getReferenceById(productId)
}
