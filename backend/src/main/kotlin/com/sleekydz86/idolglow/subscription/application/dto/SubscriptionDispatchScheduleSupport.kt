package com.sleekydz86.idolglow.subscription.application.dto

import com.sleekydz86.idolglow.subscription.domain.SubscriptionDispatchSchedule
import java.time.LocalDateTime

@Suppress("ReturnCount", "MagicNumber")
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
