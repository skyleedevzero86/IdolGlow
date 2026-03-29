package com.sleekydz86.idolglow.productpackage.product.graphql

import com.sleekydz86.idolglow.productpackage.product.application.ProductQueryService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class ProductGraphQlController(
    private val productQueryService: ProductQueryService,
) {

    @QueryMapping
    fun health(): String = "up"

    @QueryMapping
    fun products(
        @Argument lastId: String?,
        @Argument size: Int?,
        @Argument tag: String?,
    ): ProductSliceGraphQlResponse {
        val pageSize = (size ?: DEFAULT_PAGE_SIZE).coerceIn(1, MAX_PAGE_SIZE)
        val parsedLastId = when {
            lastId.isNullOrBlank() -> null
            else -> lastId.toLongOrNull()
                ?: throw IllegalArgumentException("lastId는 숫자여야 합니다.")
        }

        val items = productQueryService.findProductsByNoOffset(parsedLastId, pageSize, tag)
            .map(ProductSummaryGraphQlResponse::from)

        val nextCursor = items.takeIf { it.size == pageSize }?.lastOrNull()?.id
        return ProductSliceGraphQlResponse(
            items = items,
            nextCursor = nextCursor
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
