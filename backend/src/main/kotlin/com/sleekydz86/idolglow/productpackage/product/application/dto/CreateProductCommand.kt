package com.sleekydz86.idolglow.productpackage.product.application.dto

import java.time.LocalDate

data class CreateProductCommand(
    val name: String,
    val description: String,
    val slotStartDate: LocalDate?, // 상품 생성 시 예약 시작 가능한 날짜 (기본: 오늘+1일)
    val slotEndDate: LocalDate?, // 상품 생성 시 예약 마감 날짜 (기본: 오늘+12개월)
    val slotStartHour: Int = 9, // 예약 가능한 타임 슬롯 시작 지점
    val slotEndHour: Int = 16, // 예약 가능한 타임 슬롯 마지막 지점
    val optionIds: List<Long> = emptyList(), // 예약에 포함할 옵션 IDs
    val tagNames: List<String> = emptyList(), // 상품에 해당하는 tags
    val location: ProductLocationPayload? = null,
)