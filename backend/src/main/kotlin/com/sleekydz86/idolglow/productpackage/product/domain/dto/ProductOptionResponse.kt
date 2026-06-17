package com.sleekydz86.idolglow.productpackage.product.domain.dto

import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

data class ProductOptionResponse(
    @field:Schema(description = "옵션 ID", example = "1")
    val id: Long,

    @field:Schema(description = "옵션명", example = "스파 이용권")
    val name: String,

    @field:Schema(description = "옵션 설명", example = "프라이빗 스파 2시간 이용")
    val description: String,

    @field:Schema(description = "가격", example = "100000.00")
    val price: BigDecimal,

    @field:Schema(description = "장소", example = "서울")
    val location: String,

    @field:Schema(description = "옵션별 이미지 URL 목록(sort_order 순)")
    val imageUrls: List<String> = emptyList(),
) {
    companion object {
        fun from(option: Option): ProductOptionResponse =
            ProductOptionResponse(
                id = option.id,
                name = option.name,
                description = option.description,
                price = option.price,
                location = option.location,
                imageUrls = emptyList(),
            )
    }
}
