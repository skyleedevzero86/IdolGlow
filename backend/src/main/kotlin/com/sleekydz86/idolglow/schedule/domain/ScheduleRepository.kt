package com.sleekydz86.idolglow.schedule.domain

import java.time.LocalDateTime

interface ScheduleRepository {

    fun save(schedule: Schedule): Schedule

    fun findByIdAndUserId(scheduleId: Long, userId: Long): Schedule?

    fun findByUserId(userId: Long, cursorId: Long?, size: Int): List<Schedule>

    fun findByReservationContext(
        userId: Long,
        productId: Long,
        startAt: LocalDateTime,
        endAt: LocalDateTime
    ): Schedule?

    fun delete(schedule: Schedule)
}
