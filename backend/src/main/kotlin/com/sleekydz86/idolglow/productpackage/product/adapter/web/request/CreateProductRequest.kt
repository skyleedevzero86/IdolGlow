package com.sleekydz86.idolglow.productpackage.product.ui.request

import com.sleekydz86.idolglow.productpackage.product.application.dto.CreateProductCommand
import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductLocationPayload
import com.sleekydz86.idolglow.productpackage.product.domain.dto.TourAttractionPickPayload
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.PositiveOrZero
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "상품 생성 요청 DTO")
data class CreateProductRequest(
    @field:NotBlank
    @field:Schema(description = "상품명", example = "성수 글로우 메이크업 패키지")
    val name: String,
    @field:NotBlank
    @field:Schema(description = "상품 설명", example = "헤어 스타일링과 메이크업이 포함된 상품입니다.")
    val description: String,
    @field:Schema(description = "상품 기본가(원, 옵션 합과 별도). 0 가능.", example = "10000.00")
    @field:PositiveOrZero
    val basePrice: BigDecimal = BigDecimal.ZERO,
    @field:Schema(description = "예약 시작 날짜", example = "2026-04-01")
    val slotStartDate: LocalDate? = null,
    @field:Schema(description = "예약 종료 날짜", example = "2026-04-30")
    val slotEndDate: LocalDate? = null,
    @field:Schema(description = "슬롯 시작 시각(시)", example = "9")
    val slotStartHour: Int = 9,
    @field:Schema(description = "슬롯 종료 시각(시)", example = "16")
    val slotEndHour: Int = 16,
    @field:Schema(description = "슬롯 시작 시각(HH:mm)", example = "09:00")
    val slotStartTime: String? = null,
    @field:Schema(description = "슬롯 종료 시각(HH:mm)", example = "16:00")
    val slotEndTime: String? = null,
    @field:Schema(description = "연결할 옵션 ID 목록", example = "[1, 2]")
    val optionIds: List<Long> = emptyList(),
    @field:Schema(description = "태그명 목록", example = "[\"글로우\", \"메이크업\"]")
    val tagNames: List<String> = emptyList(),
    @field:Schema(description = "상품 위치 정보")
    val location: ProductLocationPayload? = null,
    @field:Schema(description = "저장할 Tour 관광지 다중 선택")
    val tourAttractionPicks: List<TourAttractionPickPayload> = emptyList(),
)

fun CreateProductRequest.toCommand(): CreateProductCommand =
    CreateProductCommand(
        name = name,
        description = description,
        basePrice = basePrice,
        slotStartDate = slotStartDate,
        slotEndDate = slotEndDate,
        slotStartTime = parseTimeOrNull(slotStartTime) ?: LocalTime.of(slotStartHour, 0),
        slotEndTime = parseTimeOrNull(slotEndTime) ?: LocalTime.of(slotEndHour, 0),
        optionIds = optionIds,
        tagNames = tagNames,
        location = location,
        tourAttractionPicks = tourAttractionPicks,
    )

private fun parseTimeOrNull(value: String?): LocalTime? {
    val trimmed = value?.trim().orEmpty()
    if (trimmed.isEmpty()) {
        return null
    }
    return LocalTime.parse(trimmed)
}
