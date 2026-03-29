package com.sleekydz86.idolglow.productpackage.product.application.dto

import java.time.LocalDate

data class CreateProductCommand(
    val name: String,
    val description: String,
    val slotStartDate: LocalDate?,
    val slotEndDate: LocalDate?,
    val slotStartHour: Int = 9,
    val slotEndHour: Int = 16,
    val optionIds: List<Long> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val location: ProductLocationPayload? = null,
)
