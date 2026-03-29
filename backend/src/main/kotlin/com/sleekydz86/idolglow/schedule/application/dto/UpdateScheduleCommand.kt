package com.sleekydz86.idolglow.schedule.application.dto

import java.time.LocalDateTime

data class UpdateScheduleCommand(
    val scheduleId: Long,
    val userId: Long,
    val title: String,
    val startAt: LocalDateTime,
    val endAt: LocalDateTime
)