package com.sleekydz86.idolglow.schedule.domain.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ScheduleSliceResponse(
    @Schema(description = "일정 목록")
    val schedules: List<ScheduleResponse>,
    @Schema(description = "다음 페이지 존재 여부", example = "true")
    val hasNext: Boolean,
    @Schema(description = "다음 페이지 조회에 사용할 마지막 일정 ID", example = "10")
    val nextCursorId: Long?
)