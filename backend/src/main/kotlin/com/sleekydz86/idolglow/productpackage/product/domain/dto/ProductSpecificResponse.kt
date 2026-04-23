package com.sleekydz86.idolglow.productpackage.product.domain.dto

import com.sleekydz86.idolglow.productpackage.option.domain.Option
import com.sleekydz86.idolglow.productpackage.product.domain.Product
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalTime

@Schema(description = "상품 상세 조회 응답 DTO")
data class ProductSpecificResponse(
    @field:Schema(description = "상품 ID", example = "1")
    val id: Long,

    @field:Schema(description = "상품명", example = "스파 + 브런치 패키지")
    val name: String,

    @field:Schema(description = "상품 설명", example = "스파 이용권과 브런치 코스가 포함된 상품입니다.")
    val description: String,

    @field:Schema(description = "포함 옵션 목록")
    val options: List<ProductOptionResponse>,

    @field:Schema(description = "태그 목록", example = "[\"스파\", \"브런치\"]")
    val tagNames: List<String>,

    @field:Schema(description = "예약 시작 가능 날짜", example = "2025-01-01")
    val slotStartDate: LocalDate?,

    @field:Schema(description = "예약 마감 날짜", example = "2025-01-07")
    val slotEndDate: LocalDate?,

    @field:Schema(description = "예약 슬롯 시작 시간", example = "09:00:00")
    val slotStartTime: LocalTime?,

    @field:Schema(description = "예약 슬롯 종료 시간", example = "16:00:00")
    val slotEndTime: LocalTime?,

    @field:Schema(description = "예약 슬롯 개수", example = "56")
    val reservationSlotCount: Int,

    @field:Schema(description = "최소 가격", example = "100000.00")
    val minPrice: BigDecimal,

    @field:Schema(description = "총 가격", example = "300000.00")
    val totalPrice: BigDecimal,

    @field:Schema(description = "대표 위치(지도)")
    val location: ProductLocationSummaryResponse?,

    @field:Schema(description = "대표 썸네일 URL(images 중 최소 sort_order)")
    val thumbnailUrl: String? = null,

    @field:Schema(description = "상품 상세 갤러리 이미지 URL 목록(sort_order 순)")
    val imageUrls: List<String> = emptyList(),

    @field:Schema(description = "저장된 Tour 관광지 다중 선택")
    val tourAttractionPicks: List<TourAttractionPickPayload> = emptyList(),
) {
    companion object {
        fun from(
            product: Product,
            tourAttractionPicks: List<TourAttractionPickPayload> = emptyList(),
        ): ProductSpecificResponse {
            val slotStartDate: LocalDate? = product.reservationSlots.minOfOrNull { it.reservationDate }
            val slotEndDate: LocalDate? = product.reservationSlots.maxOfOrNull { it.reservationDate }
            val slotStartTime: LocalTime? = product.reservationSlots.minByOrNull { it.startTime }?.startTime
            val slotEndTime: LocalTime? = product.reservationSlots.maxByOrNull { it.endTime }?.endTime

            return ProductSpecificResponse(
                id = product.id,
                name = product.name,
                description = product.description,
                options = product.productOptions.map { ProductOptionResponse.from(it.option) },
                tagNames = product.productTags.map { it.tagName }.distinct(),
                slotStartDate = slotStartDate,
                slotEndDate = slotEndDate,
                slotStartTime = slotStartTime,
                slotEndTime = slotEndTime,
                reservationSlotCount = product.reservationSlots.size,
                minPrice = product.minPrice,
                totalPrice = product.totalPrice,
                location = product.productLocation?.let { ProductLocationSummaryResponse.from(it) },
                tourAttractionPicks = tourAttractionPicks,
            )
        }
    }
}

@Schema(description = "상품 포함 옵션 응답 DTO")
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
