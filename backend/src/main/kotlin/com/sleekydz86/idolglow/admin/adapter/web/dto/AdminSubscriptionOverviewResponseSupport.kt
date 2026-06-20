package com.sleekydz86.idolglow.admin.adapter.web.dto

import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import java.time.DayOfWeek
import java.time.LocalDateTime

internal fun DayOfWeek.toKoreanLabel(): String =
    when (this) {
        DayOfWeek.MONDAY -> "월요일"
        DayOfWeek.TUESDAY -> "화요일"
        DayOfWeek.WEDNESDAY -> "수요일"
        DayOfWeek.THURSDAY -> "목요일"
        DayOfWeek.FRIDAY -> "금요일"
        DayOfWeek.SATURDAY -> "토요일"
        DayOfWeek.SUNDAY -> "일요일"
    }

internal fun SubscriptionDispatchSchedule.nextDispatchDateTime(now: LocalDateTime = LocalDateTime.now()): LocalDateTime? {
    if (!active) return null

    val baseCandidate =
        now
            .withHour(dispatchHour)
            .withMinute(dispatchMinute)
            .withSecond(0)
            .withNano(0)

    val candidate =
        when (frequencyType.name) {
            "DAILY" -> if (baseCandidate.isAfter(now)) baseCandidate else baseCandidate.plusDays(1)
            else -> {
                val target = dayOfWeek ?: return null
                val daysUntil = ((target.value - now.dayOfWeek.value) + 7) % 7
                val initial = baseCandidate.plusDays(daysUntil.toLong())
                if (daysUntil == 0 && !initial.isAfter(now)) initial.plusWeeks(1) else initial
            }
        }

    return candidate
}
