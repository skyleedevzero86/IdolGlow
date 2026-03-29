package com.sleekydz86.idolglow.schedule.ui.dto

import io.swagger.v3.oas.annotations.media.Schema

data class ScheduleCalendarExportResponse(
    @Schema(description = "Google Calendar에 바로 추가할 수 있는 템플릿 URL (알람은 URL로 지정 불가; .ics 권장)")
    val googleCalendarUrl: String,
    @Schema(description = "인증된 GET으로 .ics를 받는 API 경로 (VALARM: D-1, 3시간 전, 1시간 전 포함)")
    val icsRelativePath: String,
)
