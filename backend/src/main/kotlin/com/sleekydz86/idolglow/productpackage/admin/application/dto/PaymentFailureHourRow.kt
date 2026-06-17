package com.sleekydz86.idolglow.productpackage.admin.application.dto

data class PaymentFailureHourRow(
    val hourOfDay: Int,
    val failureCount: Long,
)
