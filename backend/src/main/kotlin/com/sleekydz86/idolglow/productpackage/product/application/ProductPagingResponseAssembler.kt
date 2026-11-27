package com.sleekydz86.idolglow.productpackage.product.application

import com.sleekydz86.idolglow.image.application.AggregateImageQueryService
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import com.sleekydz86.idolglow.productpackage.product.domain.dto.ProductPagingQueryResponse
import org.springframework.stereotype.Component

@Component
class ProductPagingResponseAssembler(
    private val aggregateImageQueryService: AggregateImageQueryService,
    private val productEventCalendar: ProductEventCalendar,
) {
    fun assemble(products: List<Product>): List<ProductPagingQueryResponse> {
        if (products.isEmpty()) return emptyList()
        val thumbnailById = aggregateImageQueryService.firstProductImageUrlByProductIds(products.map { it.id })
        val today = productEventCalendar.today()
        return products.map { product ->
            ProductPagingQueryResponse.from(
                product = product,
                today = today,
                thumbnailUrl = thumbnailById[product.id],
                tourAttractionPickCount = if (product.tourAttractionPicksJson.isNullOrBlank()) 0 else 1,
            )
        }
    }
}
