package com.sleekydz86.idolglow.productpackage.option.application.dto

import com.sleekydz86.idolglow.productpackage.option.domain.Option
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal

@Schema(description = "옵션 응답 DTO")
data class OptionResponse(
    @field:Schema(description = "옵션 ID", example = "1")
    val id: Long,

    @field:Schema(description = "옵션명", example = "뷰티 스파 이용권")
    val name: String,

    @field:Schema(description = "옵션 설명", example = "프라이빗 뷰티 스파 2시간 이용")
    val description: String,

    @field:Schema(description = "가격", example = "100000.00")
    val price: BigDecimal,

    @field:Schema(description = "장소", example = "서울")
    val location: String,
) {
    companion object {
        fun from(option: Option): OptionResponse =
            OptionResponse(
                id = option.id,
                name = option.name,
                description = option.description,
                price = option.price,
                location = option.location
            )
    }
}
