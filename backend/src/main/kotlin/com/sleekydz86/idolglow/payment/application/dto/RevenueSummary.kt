package com.sleekydz86.idolglow.payment.application.dto

data class RevenueSummary(
    val thisMonthNetAmount: Long,
    val billingMonthlyAmount: Long,
    val expectedMonthlyAmount: Long,
    val activeBillingCount: Long,
)
