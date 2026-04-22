package com.sleekydz86.idolglow.productpackage.admin.application.dto

data class OperationsMenuStatsResponse(
    val productsCount: Long,
    val optionsCount: Long,
    val slotsCount: Long,
    val reservationsPendingCount: Long,
    val reservationsBookedCount: Long,
    val reservationsCompletedCount: Long,
    val reservationsCanceledCount: Long,
    val paymentsPendingCount: Long,
    val paymentsSucceededCount: Long,
    val paymentsFailedCount: Long,
    val paymentsCanceledCount: Long,
    val paymentsExpiredCount: Long,
    val paymentsRefundedCount: Long,
    val paymentsPartialCanceledCount: Long,
)
