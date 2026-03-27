package com.sleekydz86.idolglow.productpackage.product.ui.request

import com.sleekydz86.idolglow.productpackage.product.application.dto.CreateProductCommand
import com.sleekydz86.idolglow.productpackage.product.application.dto.ProductLocationPayload
import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

data class CreateProductRequest(
    @field:NotBlank
    val name: String,
    @field:NotBlank
    val description: String,
    val slotStartDate: LocalDate? = null,
    val slotEndDate: LocalDate? = null,
    val slotStartHour: Int = 9,
    val slotEndHour: Int = 16,
    val optionIds: List<Long> = emptyList(),
    val tagNames: List<String> = emptyList(),
    val location: ProductLocationPayload? = null,
)

fun CreateProductRequest.toCommand(): CreateProductCommand =
    CreateProductCommand(
        name = name,
        description = description,
        slotStartDate = slotStartDate,
        slotEndDate = slotEndDate,
        slotStartHour = slotStartHour,
        slotEndHour = slotEndHour,
        optionIds = optionIds,
        tagNames = tagNames,
        location = location,
    )
