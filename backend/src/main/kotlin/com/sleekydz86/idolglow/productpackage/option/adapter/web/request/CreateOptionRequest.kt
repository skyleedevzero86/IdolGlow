package com.sleekydz86.idolglow.productpackage.option.ui.request

import com.sleekydz86.idolglow.productpackage.option.application.dto.CreateOptionCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal

@Schema(description = "옵션 생성 요청 DTO")
data class CreateOptionRequest(
    @field:Schema(description = "옵션명", example = "뷰티 스파 이용권")
    @field:NotBlank
    val name: String,

    @field:Schema(description = "옵션 설명", example = "프라이빗 뷰티 스파 2시간 이용")
    @field:NotBlank
    val description: String,

    @field:Schema(
        description = "추가 가격(원). 0이면 이 옵션에 대한 추가 요금은 없고, 상품·패키지 가격(연결된 옵션 합)만 반영됩니다.",
        example = "100000.00",
    )
    @field:PositiveOrZero
    val price: BigDecimal,

    @field:Schema(description = "장소", example = "서울")
    @field:NotBlank
    val location: String,
)

fun CreateOptionRequest.toCommand(): CreateOptionCommand =
    CreateOptionCommand(
        name = name,
        description = description,
        price = price,
        location = location
    )
