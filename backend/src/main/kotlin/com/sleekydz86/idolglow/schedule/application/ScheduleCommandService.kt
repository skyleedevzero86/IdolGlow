package com.sleekydz86.idolglow.schedule.application

import com.sleekydz86.idolglow.schedule.application.dto.CreateScheduleCommand
import com.sleekydz86.idolglow.schedule.application.dto.UpdateScheduleCommand
import com.sleekydz86.idolglow.schedule.domain.Schedule
import com.sleekydz86.idolglow.schedule.domain.ScheduleRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Transactional
@Service
class ScheduleCommandService(
    private val scheduleRepository: ScheduleRepository
) {

    fun createSchedule(command: CreateScheduleCommand): Schedule {
        val schedule = Schedule.of(
            userId = command.userId,
            productId = command.productId,
            title = command.title,
            startAt = command.startAt,
            endAt = command.endAt
        )
        return scheduleRepository.save(schedule)
    }

    fun updateSchedule(command: UpdateScheduleCommand): Schedule {
        val schedule = findUserSchedule(command.scheduleId, command.userId)
        schedule.update(
            title = command.title,
            startAt = command.startAt,
            endAt = command.endAt
        )
        return schedule
    }

    fun deleteSchedule(scheduleId: Long, userId: Long) {
        val schedule = findUserSchedule(scheduleId, userId)
        scheduleRepository.delete(schedule)
    }

    private fun findUserSchedule(scheduleId: Long, userId: Long): Schedule =
        scheduleRepository.findByIdAndUserId(scheduleId, userId)
            ?: throw IllegalArgumentException("Schedule $scheduleId not found.")
}
