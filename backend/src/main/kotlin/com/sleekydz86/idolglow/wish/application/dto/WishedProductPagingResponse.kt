package com.sleekydz86.idolglow.wish.application.dto

import com.sleekydz86.idolglow.productpackage.product.domain.Product
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "위시 상품 목록 응답 DTO")
data class WishedProductPagingResponse(
    @field:Schema(description = "상품 ID", example = "1")
    val id: Long,

    @field:Schema(description = "상품명", example = "뷰티 올인원 패키지")
    val name: String,

    @field:Schema(description = "최저가", example = "100000.00")
    val minPrice: BigDecimal,

    @field:Schema(description = "태그 목록", example = "[\"뷰티\",\"헤어\"]")
    val tagNames: List<String>,

    @field:Schema(description = "상품 대표 이미지 URL(없으면 null)")
    val thumbnailUrl: String? = null,
) {
    companion object {
        fun from(
            product: Product,
            tagNames: List<String> = product.productTags.map { it.tagName }.distinct(),
            thumbnailUrl: String? = null,
        ): WishedProductPagingResponse =
            WishedProductPagingResponse(
                id = product.id,
                name = product.name,
                minPrice = product.minPrice,
                tagNames = tagNames,
                thumbnailUrl = thumbnailUrl,
            )
    }
}
