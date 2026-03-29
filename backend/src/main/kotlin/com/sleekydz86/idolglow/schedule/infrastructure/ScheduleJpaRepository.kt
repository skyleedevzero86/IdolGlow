package com.sleekydz86.idolglow.schedule.infrastructure

import com.sleekydz86.idolglow.schedule.domain.Schedule
import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDateTime

interface ScheduleJpaRepository : JpaRepository<Schedule, Long> {

    fun findByIdAndUserId(scheduleId: Long, userId: Long): Schedule?

    fun findByUserIdAndProductIdAndStartAtAndEndAt(
        userId: Long,
        productId: Long,
        startAt: LocalDateTime,
        endAt: LocalDateTime
    ): Schedule?
}
