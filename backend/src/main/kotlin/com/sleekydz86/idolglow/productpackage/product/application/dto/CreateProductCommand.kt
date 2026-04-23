package com.sleekydz86.idolglow.productpackage.product.application.dto

import com.sleekydz86.idolglow.productpackage.product.domain.dto.TourAttractionPickPayload
import java.time.LocalDate
import java.time.LocalTime

data class CreateProductCommand(
    val name: String,
    val description: String,
    val slotStartDate: LocalDate?,
    val slotEndDate: LocalDate?,
    val slotStartTime: LocalTime = LocalTime.of(9, 0),
    val slotEndTime: LocalTime = LocalTime.of(16, 0),
    val optionIds: List<Long> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val location: ProductLocationPayload? = null,
    val tourAttractionPicks: List<TourAttractionPickPayload> = emptyList(),
)
