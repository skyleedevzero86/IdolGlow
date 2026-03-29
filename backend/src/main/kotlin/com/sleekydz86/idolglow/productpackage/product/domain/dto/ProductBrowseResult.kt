package com.sleekydz86.idolglow.productpackage.product.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "상품 목록 슬라이스(페이지네이션 메타 포함)")
data class ProductBrowseResult(
    @field:Schema(description = "상품 요약 목록")
    val items: List<ProductPagingQueryResponse>,

    @field:Schema(description = "다음 페이지 커서(최신순 정렬일 때만, 상품 ID)")
    val nextCursor: Long?,

    @field:Schema(description = "다음 페이지 offset(최신순이 아닐 때)")
    val nextOffset: Int?,
)
