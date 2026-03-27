package com.sleekydz86.idolglow.productpackage.reservation.ui.request

import com.sleekydz86.idolglow.productpackage.reservation.application.dto.CreateReservationCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.math.BigDecimal

@Schema(description = "예약 생성 요청 DTO")
data class CreateReservationRequest(
    @field:Schema(description = "예약 슬롯 ID", example = "5")
    @field:Positive
    val reservationSlotId: Long,
    @field:Schema(description = "결제 총액", example = "300000.00")
    @field:NotNull
    @field:Positive
    var totalPrice: BigDecimal
)

fun CreateReservationRequest.toCommand(userId: Long, productId: Long): CreateReservationCommand =
    CreateReservationCommand(
        productId = productId,
        reservationSlotId = reservationSlotId,
        userId = userId,
        totalPrice = totalPrice
    )
