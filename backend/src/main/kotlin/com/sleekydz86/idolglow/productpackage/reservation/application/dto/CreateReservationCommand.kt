package com.sleekydz86.idolglow.productpackage.reservation.application.dto

import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationBookingChannel
import java.math.BigDecimal

data class CreateReservationCommand(
    val productId: Long,
    val reservationSlotId: Long,
    val userId: Long,
    val totalPrice: BigDecimal,
    val bookingChannel: ReservationBookingChannel = ReservationBookingChannel.NORMAL,
)
