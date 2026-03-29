package com.sleekydz86.idolglow.productpackage.product.domain.dto

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "상품 목록 조회 응답 DTO")
data class ProductPagingQueryResponse(
    @field:Schema(description = "상품 ID", example = "1")
    val id: Long,

    @field:Schema(description = "상품명", example = "뷰티 사인 패키지")
    val name: String,

    @field:Schema(description = "상품 설명", example = "메이크업과 헤어 스타일링이 포함된 패키지")
    val description: String,

    @field:Schema(description = "최소 가격", example = "100000.00")
    val minPrice: BigDecimal,

    @field:Schema(description = "총 가격", example = "300000.00")
    val totalPrice: BigDecimal,

    @field:Schema(description = "태그 목록", example = "[\"뷰티\", \"메이크업\"]")
    val tagNames: List<String>,
) {
    companion object {
        fun from(
            product: Product,
            tagNames: List<String> = product.productTags.map { it.tagName }.distinct(),
        ): ProductPagingQueryResponse =
            ProductPagingQueryResponse(
                id = product.id,
                name = product.name,
                description = product.description,
                minPrice = product.minPrice,
                totalPrice = product.totalPrice,
                tagNames = tagNames
            )
    }
}
