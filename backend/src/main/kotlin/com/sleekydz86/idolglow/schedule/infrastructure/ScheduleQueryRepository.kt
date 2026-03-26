package com.sleekydz86.idolglow.schedule.infrastructure

import com.querydsl.jpa.impl.JPAQueryFactory
import com.sleekydz86.idolglow.schedule.domain.Schedule
import org.springframework.stereotype.Repository

@Repository
class ScheduleQueryRepository(
    private val queryFactory: JPAQueryFactory
) {

    private val schedule = QSchedule.schedule

    fun findByIdAndUserId(scheduleId: Long, userId: Long): Schedule? =
        queryFactory.selectFrom(schedule)
            .where(
                schedule.id.eq(scheduleId),
                schedule.userId.eq(userId)
            )
            .fetchOne()

    fun findByUserId(userId: Long, cursorId: Long?, size: Int): List<Schedule> {
        val cursorSchedule = cursorId?.let { findByIdAndUserId(it, userId) }
        if (cursorId != null && cursorSchedule == null) {
            throw IllegalArgumentException("Schedule $cursorId not found.")
        }

        val predicate = schedule.userId.eq(userId).let { base ->
            cursorSchedule?.let {
                base.and(
                    schedule.startAt.lt(it.startAt)
                        .or(schedule.startAt.eq(it.startAt).and(schedule.id.lt(it.id)))
                )
            } ?: base
        }

        return queryFactory.selectFrom(schedule)
            .where(predicate)
            .orderBy(schedule.startAt.desc(), schedule.id.desc())
            .limit((size + 1).toLong())
            .fetch()
    }
}
