package com.sleekydz86.idolglow.schedule.graphql

import com.sleekydz86.idolglow.global.graphql.toGraphQlIdLong
import com.sleekydz86.idolglow.global.resolver.AuthenticatedUserIdResolver
import com.sleekydz86.idolglow.schedule.application.ScheduleCommandService
import com.sleekydz86.idolglow.schedule.application.ScheduleExternalCalendarService
import com.sleekydz86.idolglow.schedule.application.ScheduleQueryService
import com.sleekydz86.idolglow.schedule.ui.dto.ScheduleCommandResponse
import com.sleekydz86.idolglow.schedule.ui.request.CreateScheduleRequest
import com.sleekydz86.idolglow.schedule.ui.request.UpdateScheduleRequest
import com.sleekydz86.idolglow.schedule.ui.request.toCommand
import jakarta.validation.Valid
import org.springframework.graphql.data.method.annotation.Argument
import org.springframework.graphql.data.method.annotation.MutationMapping
import org.springframework.graphql.data.method.annotation.QueryMapping
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.stereotype.Controller

@Controller
@PreAuthorize("isAuthenticated()")
class ScheduleGraphQlController(
    private val scheduleCommandService: ScheduleCommandService,
    private val scheduleQueryService: ScheduleQueryService,
    private val scheduleExternalCalendarService: ScheduleExternalCalendarService,
    private val authenticatedUserIdResolver: AuthenticatedUserIdResolver,
) {

    @QueryMapping
    fun schedules(
        @Argument cursorId: String?,
        @Argument size: Int?,
    ): ScheduleSliceGraphQlResponse =
        ScheduleSliceGraphQlResponse.from(
            scheduleQueryService.findSchedules(
                userId = authenticatedUserIdResolver.resolveRequired(),
                cursorId = cursorId?.takeIf { it.isNotBlank() }?.toGraphQlIdLong("cursorId"),
                size = (size ?: 20).coerceIn(1, 50)
            )
        )

    @QueryMapping
    fun schedule(@Argument id: String): ScheduleGraphQlResponse =
        ScheduleGraphQlResponse.from(
            scheduleQueryService.findSchedule(
                scheduleId = id.toGraphQlIdLong("id"),
                userId = authenticatedUserIdResolver.resolveRequired()
            )
        )

    @QueryMapping
    fun scheduleCalendarExport(@Argument scheduleId: String): ScheduleCalendarExportGraphQlResponse {
        val userId = authenticatedUserIdResolver.resolveRequired()
        val schedule = scheduleQueryService.findSchedule(
            scheduleId = scheduleId.toGraphQlIdLong("scheduleId"),
            userId = userId,
        )
        return ScheduleCalendarExportGraphQlResponse.from(
            scheduleExternalCalendarService.buildExportResponse(schedule)
        )
    }

    @MutationMapping
    fun createSchedule(@Argument @Valid input: CreateScheduleRequest): ScheduleGraphQlResponse =
        ScheduleGraphQlResponse.from(
            ScheduleCommandResponse.from(
                scheduleCommandService.createSchedule(input.toCommand(authenticatedUserIdResolver.resolveRequired()))
            )
        )

    @MutationMapping
    fun updateSchedule(
        @Argument scheduleId: String,
        @Argument @Valid input: UpdateScheduleRequest,
    ): ScheduleGraphQlResponse =
        ScheduleGraphQlResponse.from(
            ScheduleCommandResponse.from(
                scheduleCommandService.updateSchedule(
                    input.toCommand(
                        userId = authenticatedUserIdResolver.resolveRequired(),
                        scheduleId = scheduleId.toGraphQlIdLong("scheduleId")
                    )
                )
            )
        )

    @MutationMapping
    fun deleteSchedule(@Argument scheduleId: String): Boolean {
        scheduleCommandService.deleteSchedule(
            scheduleId = scheduleId.toGraphQlIdLong("scheduleId"),
            userId = authenticatedUserIdResolver.resolveRequired()
        )
        return true
    }
}
