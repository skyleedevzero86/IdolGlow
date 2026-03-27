package com.sleekydz86.idolglow.schedule.infrastructure

import com.sleekydz86.idolglow.schedule.domain.Schedule
import com.sleekydz86.idolglow.schedule.domain.ScheduleRepository
import org.springframework.stereotype.Repository
import java.time.LocalDateTime

@Repository
class ScheduleRepositoryImpl(
    private val scheduleJpaRepository: ScheduleJpaRepository,
    private val scheduleQueryRepository: ScheduleQueryRepository
) : ScheduleRepository {

    override fun save(schedule: Schedule): Schedule =
        scheduleJpaRepository.save(schedule)

    override fun findByIdAndUserId(scheduleId: Long, userId: Long): Schedule? =
        scheduleQueryRepository.findByIdAndUserId(scheduleId, userId)

    override fun findByUserId(userId: Long, cursorId: Long?, size: Int): List<Schedule> {
        return scheduleQueryRepository.findByUserId(userId, cursorId, size)
    }

    override fun findByReservationContext(
        userId: Long,
        productId: Long,
        startAt: LocalDateTime,
        endAt: LocalDateTime
    ): Schedule? =
        scheduleJpaRepository.findByUserIdAndProductIdAndStartAtAndEndAt(
            userId = userId,
            productId = productId,
            startAt = startAt,
            endAt = endAt
        )

    override fun delete(schedule: Schedule) =
        scheduleJpaRepository.delete(schedule)
}
