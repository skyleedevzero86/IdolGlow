package com.sleekydz86.idolglow.schedule.application

import com.sleekydz86.idolglow.schedule.domain.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import kotlin.collections.map

@Transactional(readOnly = true)
@Service
class ScheduleQueryService(
    private val scheduleRepository: ScheduleRepository
) {

    fun findSchedule(scheduleId: Long, userId: Long): ScheduleResponse {
        val schedule = scheduleRepository.findByIdAndUserId(scheduleId, userId)
            ?: throw IllegalArgumentException("Schedule $scheduleId not found.")
        return ScheduleResponse.from(schedule)
    }

    fun findSchedules(userId: Long, cursorId: Long?, size: Int): ScheduleSliceResponse {
        val schedules = scheduleRepository.findByUserId(userId, cursorId, size)
        val hasNext = schedules.size > size
        val content = if (hasNext) schedules.subList(0, size) else schedules
        val nextCursorId = if (hasNext) content.last().id else null

        return ScheduleSliceResponse(
            schedules = content.map { ScheduleResponse.from(it) },
            hasNext = hasNext,
            nextCursorId = nextCursorId
        )
    }
}
