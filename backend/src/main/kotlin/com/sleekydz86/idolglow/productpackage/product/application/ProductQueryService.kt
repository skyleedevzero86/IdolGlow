package com.sleekydz86.idolglow.productpackage.product.application

import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductBrowseParams
import com.sleekydz86.idolglow.productpackage.product.domain.ProductRepository
import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductBrowseResult
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSearchCriteria
import com.sleekydz86.idolglow.image.application.AggregateImageQueryService
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Transactional(readOnly = true)
@Service
class ProductQueryService(
    private val productRepository: ProductRepository,
    private val aggregateImageQueryService: AggregateImageQueryService,
) {

    fun findProductsByNoOffset(
        lastId: Long?,
        size: Int,
        tagName: String?,
    ): List<ProductPagingQueryResponse> {
        val list = productRepository.findProductsByNoOffset(
            lastId = lastId,
            size = size,
            tagName = tagName
        )
        val thumbs = aggregateImageQueryService.firstProductImageUrlByProductIds(list.map { it.id })
        return list.map { it.copy(thumbnailUrl = thumbs[it.id]) }
    }

    fun browseProducts(params: ProductBrowseParams): ProductBrowseResult {
        var sort = params.sort
        if (sort == ProductSort.DISTANCE && (params.nearLatitude == null || params.nearLongitude == null)) {
            sort = ProductSort.NEWEST
        }
        val offset = (params.offset ?: 0).coerceIn(0, MAX_BROWSE_OFFSET)
        val keyword = params.keyword?.trim()?.take(MAX_KEYWORD_LENGTH)?.takeIf { it.isNotEmpty() }
        if (params.minPrice != null && params.maxPrice != null && params.minPrice > params.maxPrice) {
            throw IllegalArgumentException("minPrice는 maxPrice보다 클 수 없습니다.")
        }
        val radius = params.radiusMeters?.coerceIn(1, MAX_RADIUS_METERS)
        val criteria = ProductSearchCriteria(
            lastId = if (sort == ProductSort.NEWEST) params.lastId else null,
            offset = if (sort == ProductSort.NEWEST) 0 else offset,
            size = params.size,
            tag = params.tag,
            tags = params.tags,
            keyword = keyword,
            minPrice = params.minPrice,
            maxPrice = params.maxPrice,
            visitDate = params.visitDate,
            reservableOnly = params.reservableOnly,
            sort = sort,
            nearLatitude = params.nearLatitude,
            nearLongitude = params.nearLongitude,
            radiusMeters = radius,
            now = LocalDateTime.now(ZoneOffset.UTC),
            today = LocalDate.now(ZoneOffset.UTC),
        )
        val slice = productRepository.browseProducts(criteria)
        val thumbs = aggregateImageQueryService.firstProductImageUrlByProductIds(slice.items.map { it.id })
        return slice.copy(
            items = slice.items.map { it.copy(thumbnailUrl = thumbs[it.id]) },
        )
    }

    fun findProductSpecificById(productId: Long): ProductSpecificResponse {
        val base = productRepository.findProductSpecificById(productId)
            ?: throw IllegalArgumentException("상품을 찾을 수 없습니다. productId=$productId")
        val gallery = aggregateImageQueryService.orderedProductImageUrls(productId)
        val optionIds = base.options.map { it.id }
        val optionImages = aggregateImageQueryService.orderedOptionImageUrlsByOptionIds(optionIds)
        return base.copy(
            thumbnailUrl = gallery.firstOrNull(),
            imageUrls = gallery,
            options = base.options.map { opt ->
                opt.copy(imageUrls = optionImages[opt.id].orEmpty())
            },
        )
    }

    companion object {
        private const val MAX_BROWSE_OFFSET = 500
        private const val MAX_KEYWORD_LENGTH = 200
        private const val MAX_RADIUS_METERS = 200_000
    }
}
