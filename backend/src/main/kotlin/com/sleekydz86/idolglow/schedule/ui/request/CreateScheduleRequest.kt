package com.sleekydz86.idolglow.schedule.ui.request

import com.sleekydz86.idolglow.schedule.application.dto.CreateScheduleCommand
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDateTime

data class CreateScheduleRequest(
    @field:NotNull
    @field:Schema(description = "상품 ID", example = "1")
    val productId: Long,
    @field:NotBlank
    @field:Schema(description = "일정 제목", example = "스튜디오 방문")
    val title: String,
    @field:NotNull
    @field:Schema(description = "일정 시작 일시", example = "2025-05-01T09:00:00")
    val startAt: LocalDateTime,
    @field:NotNull
    @field:Schema(description = "일정 종료 일시", example = "2025-05-01T10:00:00")
    val endAt: LocalDateTime
)

fun CreateScheduleRequest.toCommand(userId: Long): CreateScheduleCommand =
    CreateScheduleCommand(
        userId = userId,
        productId = productId,
        title = title,
        startAt = startAt,
        endAt = endAt
    )
