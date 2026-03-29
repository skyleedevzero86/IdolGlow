package com.sleekydz86.idolglow.schedule.infrastructure

import com.sleekydz86.idolglow.schedule.domain.Schedule
import jakarta.persistence.EntityManager
import jakarta.persistence.PersistenceContext
import org.springframework.stereotype.Repository

@Repository
class ScheduleQueryRepository {

    @PersistenceContext
    private lateinit var entityManager: EntityManager

    fun findByIdAndUserId(scheduleId: Long, userId: Long): Schedule? {
        return entityManager.createQuery(
            """
            select s
            from Schedule s
            where s.id = :scheduleId
              and s.userId = :userId
            """.trimIndent(),
            Schedule::class.java
        )
            .setParameter("scheduleId", scheduleId)
            .setParameter("userId", userId)
            .resultList
            .firstOrNull()
    }

    fun findByUserId(userId: Long, cursorId: Long?, size: Int): List<Schedule> {
        val cursorSchedule = cursorId?.let { findByIdAndUserId(it, userId) }
        if (cursorId != null && cursorSchedule == null) {
            throw IllegalArgumentException("일정을 찾을 수 없습니다. scheduleId=$cursorId")
        }

        val queryString = buildString {
            appendLine("select s")
            appendLine("from Schedule s")
            appendLine("where s.userId = :userId")
            if (cursorSchedule != null) {
                appendLine("  and (s.startAt < :cursorStartAt or (s.startAt = :cursorStartAt and s.id < :cursorId))")
            }
            appendLine("order by s.startAt desc, s.id desc")
        }

        val query = entityManager.createQuery(queryString, Schedule::class.java)
            .setParameter("userId", userId)
            .setMaxResults(size + 1)

        if (cursorSchedule != null) {
            query.setParameter("cursorStartAt", cursorSchedule.startAt)
            query.setParameter("cursorId", cursorSchedule.id)
        }

        return query.resultList
    }
}
