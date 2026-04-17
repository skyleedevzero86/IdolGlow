package com.sleekydz86.idolglow.schedule.ui

import com.sleekydz86.idolglow.schedule.domain.dto.ScheduleResponse
import com.sleekydz86.idolglow.schedule.domain.dto.ScheduleSliceResponse
import com.sleekydz86.idolglow.schedule.ui.dto.ScheduleCalendarExportResponse
import com.sleekydz86.idolglow.schedule.ui.dto.ScheduleCommandResponse
import com.sleekydz86.idolglow.schedule.ui.request.CreateScheduleRequest
import com.sleekydz86.idolglow.schedule.ui.request.UpdateScheduleRequest
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.core.io.Resource
import org.springframework.http.ResponseEntity

@Tag(name = "일정", description = "마이페이지 일정 관리 API")
interface ScheduleApi {

    @Operation(summary = "일정 생성", description = "개인 일정을 추가합니다.")
    @ApiResponse(responseCode = "201", description = "생성 성공")
    fun createSchedule(
        @Parameter(hidden = true)
        userId: Long,
        @Valid
        request: CreateScheduleRequest
    ): ResponseEntity<ScheduleCommandResponse>

    @Operation(summary = "일정 수정", description = "기존 일정을 수정합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "수정 성공"),
            ApiResponse(
                responseCode = "404",
                description = "일정을 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun updateSchedule(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "수정할 일정 ID", example = "1")
        scheduleId: Long,
        @Valid
        request: UpdateScheduleRequest
    ): ResponseEntity<ScheduleCommandResponse>

    @Operation(summary = "일정 삭제", description = "지정한 일정을 삭제합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "204", description = "삭제 성공"),
            ApiResponse(
                responseCode = "404",
                description = "일정을 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun deleteSchedule(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "삭제할 일정 ID", example = "1")
        scheduleId: Long
    ): ResponseEntity<Void>

    @Operation(summary = "일정 상세 조회", description = "개인 일정을 상세 조회합니다.")
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "일정을 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun findSchedule(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "조회할 일정 ID", example = "1")
        scheduleId: Long
    ): ResponseEntity<ScheduleResponse>

    @Operation(
        summary = "일정 목록 조회 (무한 스크롤)",
        description = "시작일시 기준 최신순으로 개인 일정 목록을 조회합니다."
    )
    @ApiResponse(responseCode = "200", description = "조회 성공")
    fun findSchedules(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "이전 페이지의 마지막 일정 ID", example = "10")
        cursorId: Long?,
        @Parameter(description = "가져올 일정 개수", example = "20")
        size: Int
    ): ResponseEntity<ScheduleSliceResponse>

    @Operation(
        summary = "외부 캘린더용 링크",
        description = "Google Calendar 템플릿 URL과 .ics 다운로드 경로를 반환합니다. .ics에는 D-1·3시간·1시간 전 알람이 포함됩니다."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "조회 성공"),
            ApiResponse(
                responseCode = "404",
                description = "일정을 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun scheduleCalendarExport(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "일정 ID", example = "1")
        scheduleId: Long,
    ): ResponseEntity<ScheduleCalendarExportResponse>

    @Operation(
        summary = "일정 .ics 다운로드",
        description = "Apple/Google 등에서 가져올 수 있는 iCalendar 파일입니다. VALARM: 하루 전, 3시간 전, 1시간 전."
    )
    @ApiResponses(
        value = [
            ApiResponse(responseCode = "200", description = "파일 반환"),
            ApiResponse(
                responseCode = "404",
                description = "일정을 찾을 수 없음",
                content = [Content(schema = Schema(hidden = true))]
            )
        ]
    )
    fun downloadScheduleIcs(
        @Parameter(hidden = true)
        userId: Long,
        @Parameter(description = "일정 ID", example = "1")
        scheduleId: Long,
    ): ResponseEntity<Resource>
}
