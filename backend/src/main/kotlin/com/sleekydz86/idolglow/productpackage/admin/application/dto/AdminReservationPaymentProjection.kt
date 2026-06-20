package com.sleekydz86.idolglow.productpackage.admin.application.dto

import com.sleekydz86.idolglow.payment.domain.PaymentStatus

data class AdminReservationPaymentProjection(
    val reservationId: Long,
    val paymentReference: String,
    val status: PaymentStatus,
    val failureReason: String?,
)
