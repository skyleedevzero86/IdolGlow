package com.sleekydz86.idolglow.schedule.application.dto

import java.time.LocalDateTime

data class CreateScheduleCommand(
    val userId: Long,
    val productId: Long,
    val title: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime
)