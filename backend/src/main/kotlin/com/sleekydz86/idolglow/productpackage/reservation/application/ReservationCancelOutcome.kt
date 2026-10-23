package com.sleekydz86.idolglow.productpackage.reservation.application

import com.sleekydz86.idolglow.payment.application.RefundPrepareResult

data class ReservationCancelOutcome(
    val reservationId: Long,
    val refund: RefundPrepareResult? = null,
)
