package com.sleekydz86.idolglow.productpackage.admin.application.dto

import java.math.BigDecimal
import java.time.LocalDate

data class PeriodCancellationMetrics(
    val fromDate: LocalDate,
    val toDate: LocalDate,
    val canceled: Long,
    val booked: Long,
    val cancelRate: BigDecimal?,
)
