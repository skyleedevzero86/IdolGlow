package com.sleekydz86.idolglow.visit.application.dto

import java.time.LocalDate

data class DailyVisitorCount(
    val date: LocalDate,
    val visitors: Long,
    val newVisitors: Long,
)

data class VisitorSummary(
    val today: Long,
    val newToday: Long,
    val daily: List<DailyVisitorCount>,
)
