package com.sleekydz86.idolglow.global.support

import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@Component
class DashboardTime {
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)

    internal var storageZone: ZoneId = ZoneId.systemDefault()

    fun today(): LocalDate = LocalDate.now(clock.withZone(IdolGlowDateTimes.ZONE))

    fun now(): LocalDateTime = LocalDateTime.now(clock.withZone(storageZone))

    fun startOf(date: LocalDate): LocalDateTime =
        date
            .atStartOfDay(IdolGlowDateTimes.ZONE)
            .withZoneSameInstant(storageZone)
            .toLocalDateTime()

    fun startOfMonth(): LocalDateTime = startOf(today().withDayOfMonth(1))

    fun hoursAgo(hours: Long): LocalDateTime = now().minusHours(hours)
}
