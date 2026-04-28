package com.sleekydz86.idolglow.schedule.ui.dto

import com.sleekydz86.idolglow.schedule.domain.Schedule
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

data class ScheduleCommandResponse(
    @Schema(description = "일정 ID", example = "1")
    val scheduleId: Long,
    @Schema(description = "상품 ID", example = "1")
    val productId: Long,
    @Schema(description = "일정 제목", example = "스튜디오 방문")
    val title: String,
    @Schema(description = "일정 시작 일시", example = "2025-05-01T09:00:00")
    val startAt: LocalDateTime,
    @Schema(description = "일정 종료 일시", example = "2025-05-01T10:00:00")
    val endAt: LocalDateTime
) {
    companion object {
        fun from(schedule: Schedule): ScheduleCommandResponse =
            ScheduleCommandResponse(
                scheduleId = schedule.id,
                productId = schedule.productId,
                title = schedule.title,
                startAt = schedule.startAt,
                endAt = schedule.endAt
            )
    }
}
