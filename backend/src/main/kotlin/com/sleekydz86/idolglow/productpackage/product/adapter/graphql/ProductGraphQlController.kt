package com.sleekydz86.idolglow.productpackage.product.graphql

import com.sleekydz86.idolglow.productpackage.product.application.ProductQueryService
import com.sleekydz86.idolglow.productpackage.product.domain.ProductSort
import com.sleekydz86.idolglow.productpackage.product.ui.ProductBrowseRequestParser
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class ProductGraphQlController(
    private val productQueryService: ProductQueryService,
) {

    @QueryMapping
    fun health(): String = "정상"

    @QueryMapping
    fun products(
        @Argument lastId: String?,
        @Argument offset: Int?,
        @Argument size: Int?,
        @Argument tag: String?,
        @Argument tags: List<String>?,
        @Argument keyword: String?,
        @Argument minPrice: String?,
        @Argument maxPrice: String?,
        @Argument visitDate: String?,
        @Argument reservableOnly: Boolean?,
        @Argument sort: ProductSort?,
        @Argument nearLatitude: String?,
        @Argument nearLongitude: String?,
        @Argument radiusMeters: Int?,
    ): ProductSliceGraphQlResponse {
        val pageSize = (size ?: DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE)
        val params = ProductBrowseRequestParser.fromGraphQl(
            lastId = lastId,
            offset = offset,
            size = pageSize,
            tag = tag,
            tags = tags,
            keyword = keyword,
            minPrice = minPrice,
            maxPrice = maxPrice,
            visitDate = visitDate,
            reservableOnly = reservableOnly,
            sort = sort,
            nearLatitude = nearLatitude,
            nearLongitude = nearLongitude,
            radiusMeters = radiusMeters,
        )
        val slice = productQueryService.browseProducts(params)
        val items = slice.items.map(ProductSummaryGraphQlResponse::from)
        val nextCursor = slice.nextCursor?.toString()
        return ProductSliceGraphQlResponse(
            items = items,
            nextCursor = nextCursor,
            nextOffset = slice.nextOffset,
        )
    }

    @QueryMapping
    fun product(@Argument id: String): ProductDetailGraphQlResponse {
        val productId = id.toLongOrNull()
            ?: throw IllegalArgumentException("id는 숫자여야 합니다.")

        return ProductDetailGraphQlResponse.from(
            productQueryService.findProductSpecificById(productId)
        )
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 50
    }
}
