package com.sleekydz86.idolglow.schedule.ui

import com.sleekydz86.idolglow.global.resolver.LoginUser
import com.sleekydz86.idolglow.schedule.application.ScheduleCommandService
import com.sleekydz86.idolglow.schedule.application.ScheduleQueryService
import com.sleekydz86.idolglow.schedule.domain.dto.ScheduleResponse
import com.sleekydz86.idolglow.schedule.domain.dto.ScheduleSliceResponse
import com.sleekydz86.idolglow.schedule.ui.dto.ScheduleCommandResponse
import com.sleekydz86.idolglow.schedule.ui.request.CreateScheduleRequest
import com.sleekydz86.idolglow.schedule.ui.request.UpdateScheduleRequest
import com.sleekydz86.idolglow.schedule.ui.request.toCommand
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.net.URI

@RequestMapping("/schedules")
@RestController
class ScheduleController(
    private val scheduleCommandService: ScheduleCommandService,
    private val scheduleQueryService: ScheduleQueryService
) : ScheduleApi {

    @PostMapping
    override fun createSchedule(
        @LoginUser userId: Long,
        @Valid @RequestBody request: CreateScheduleRequest
    ): ResponseEntity<ScheduleCommandResponse> {
        val schedule = scheduleCommandService.createSchedule(request.toCommand(userId))
        return ResponseEntity.status(HttpStatus.CREATED)
            .location(URI.create("/schedule/${schedule.id}"))
            .body(ScheduleCommandResponse.from(schedule))
    }

    @PutMapping("/{scheduleId}")
    override fun updateSchedule(
        @LoginUser userId: Long,
        @PathVariable scheduleId: Long,
        @Valid @RequestBody request: UpdateScheduleRequest
    ): ResponseEntity<ScheduleCommandResponse> {
        val updated = scheduleCommandService.updateSchedule(request.toCommand(userId, scheduleId))
        return ResponseEntity.ok(ScheduleCommandResponse.from(updated))
    }

    @DeleteMapping("/{scheduleId}")
    override fun deleteSchedule(
        @LoginUser userId: Long,
        @PathVariable scheduleId: Long
    ): ResponseEntity<Void> {
        scheduleCommandService.deleteSchedule(scheduleId, userId)
        return ResponseEntity.noContent().build()
    }

    @GetMapping("/{scheduleId}")
    override fun findSchedule(
        @LoginUser userId: Long,
        @PathVariable scheduleId: Long
    ): ResponseEntity<ScheduleResponse> =
        ResponseEntity.ok(scheduleQueryService.findSchedule(scheduleId, userId))

    @GetMapping
    override fun findSchedules(
        @LoginUser userId: Long,
        @RequestParam(required = false) cursorId: Long?,
        @RequestParam(defaultValue = "20") size: Int
    ): ResponseEntity<ScheduleSliceResponse> =
        ResponseEntity.ok(scheduleQueryService.findSchedules(userId, cursorId, size))
}
