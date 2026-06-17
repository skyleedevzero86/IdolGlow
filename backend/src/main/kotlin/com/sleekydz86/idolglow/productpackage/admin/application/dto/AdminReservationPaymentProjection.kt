package com.sleekydz86.idolglow.productpackage.admin.application.dto

import com.sleekydz86.idolglow.payment.domain.PaymentStatus
import com.sleekydz86.idolglow.productpackage.reservation.domain.Reservation
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationCancelReason
import com.sleekydz86.idolglow.productpackage.reservation.domain.ReservationStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

data class AdminReservationPaymentProjection(
    val reservationId: Long,
    val paymentReference: String,
    val status: PaymentStatus,
    val failureReason: String?,
)
