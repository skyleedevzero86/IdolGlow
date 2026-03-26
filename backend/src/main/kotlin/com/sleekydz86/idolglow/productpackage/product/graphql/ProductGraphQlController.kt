package com.sleekydz86.idolglow.productpackage.product.graphql

import com.sleekydz86.idolglow.productpackage.product.application.ProductQueryService
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductOptionResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductSpecificResponse
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
                ?: throw IllegalArgumentException("lastId must be numeric.")
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
            ?: throw IllegalArgumentException("id must be numeric.")

        return ProductDetailGraphQlResponse.from(
            productQueryService.findProductSpecificById(productId)
        )
    }

    companion object {
        private const val DEFAULT_PAGE_SIZE = 20
        private const val MAX_PAGE_SIZE = 50
    }
}

data class ProductSliceGraphQlResponse(
    val items: List<ProductSummaryGraphQlResponse>,
    val nextCursor: String?,
)

data class ProductSummaryGraphQlResponse(
    val id: String,
    val name: String,
    val description: String,
    val minPrice: String,
    val totalPrice: String,
    val tagNames: List<String>,
) {
    companion object {
        fun from(response: ProductPagingQueryResponse): ProductSummaryGraphQlResponse =
            ProductSummaryGraphQlResponse(
                id = response.id.asGraphQlId(),
                name = response.name,
                description = response.description,
                minPrice = response.minPrice.asGraphQlNumber(),
                totalPrice = response.totalPrice.asGraphQlNumber(),
                tagNames = response.tagNames
            )
    }
}

data class ProductDetailGraphQlResponse(
    val id: String,
    val name: String,
    val description: String,
    val options: List<ProductOptionGraphQlResponse>,
    val tagNames: List<String>,
    val slotStartDate: String?,
    val slotEndDate: String?,
    val slotStartTime: String?,
    val slotEndTime: String?,
    val reservationSlotCount: Int,
    val minPrice: String,
    val totalPrice: String,
) {
    companion object {
        fun from(response: ProductSpecificResponse): ProductDetailGraphQlResponse =
            ProductDetailGraphQlResponse(
                id = response.id.asGraphQlId(),
                name = response.name,
                description = response.description,
                options = response.options.map(ProductOptionGraphQlResponse::from),
                tagNames = response.tagNames,
                slotStartDate = response.slotStartDate.asGraphQlValue(),
                slotEndDate = response.slotEndDate.asGraphQlValue(),
                slotStartTime = response.slotStartTime.asGraphQlValue(),
                slotEndTime = response.slotEndTime.asGraphQlValue(),
                reservationSlotCount = response.reservationSlotCount,
                minPrice = response.minPrice.asGraphQlNumber(),
                totalPrice = response.totalPrice.asGraphQlNumber(),
            )
    }
}

data class ProductOptionGraphQlResponse(
    val id: String,
    val name: String,
    val description: String,
    val price: String,
    val location: String,
) {
    companion object {
        fun from(response: ProductOptionResponse): ProductOptionGraphQlResponse =
            ProductOptionGraphQlResponse(
                id = response.id.asGraphQlId(),
                name = response.name,
                description = response.description,
                price = response.price.asGraphQlNumber(),
                location = response.location
            )
    }
}
