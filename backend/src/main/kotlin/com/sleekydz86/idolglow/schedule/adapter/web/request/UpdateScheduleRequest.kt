package com.sleekydz86.idolglow.schedule.ui.request

import com.sleekydz86.idolglow.schedule.application.dto.UpdateScheduleCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class UpdateScheduleRequest(
    @field:NotBlank
    @field:Schema(description = "수정할 일정 제목", example = "스튜디오 방문 - 시간 확정")
    val title: String,
    @field:NotNull
    @field:Schema(description = "수정할 일정 시작 일시", example = "2025-05-01T10:00:00")
    val startAt: LocalDateTime,
    @field:NotNull
    @field:Schema(description = "수정할 일정 종료 일시", example = "2025-05-01T11:00:00")
    val endAt: LocalDateTime
)

fun UpdateScheduleRequest.toCommand(userId: Long, scheduleId: Long): UpdateScheduleCommand =
    UpdateScheduleCommand(
        scheduleId = scheduleId,
        userId = userId,
        title = title,
        startAt = startAt,
        endAt = endAt
    )
