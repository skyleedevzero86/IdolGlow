package com.sleekydz86.idolglow.productpackage.reservation.application.dto

import java.math.BigDecimal

data class CreateReservationCommand(
    val productId: Long,
    val reservationSlotId: Long,
    val userId: Long,
    val totalPrice: BigDecimal
)
