package com.sleekydz86.idolglow.productpackage.attraction.graphql

import com.sleekydz86.idolglow.productpackage.attraction.application.ProductTourAttractionQueryService
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.stereotype.Controller

@Controller
class ProductTourAttractionGraphQlController(
    private val productTourAttractionQueryService: ProductTourAttractionQueryService,
) {
    @QueryMapping
    fun productTourAttractions(
        @Argument productId: String,
        @Argument size: Int?,
        @Argument baseYm: String?,
        @Argument category: String?,
    ): ProductTourAttractionGraphQlResponse {
        val resolvedProductId = productId.toLongOrNull()
            ?: throw IllegalArgumentException("productId는 숫자여야 합니다.")

        return ProductTourAttractionGraphQlResponse.from(
            productTourAttractionQueryService.findAttractionsByProduct(
                productId = resolvedProductId,
                size = size ?: 10,
                baseYm = baseYm,
                category = category,
            )
        )
    }
}
