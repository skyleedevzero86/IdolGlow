package com.sleekydz86.idolglow.luckydraw.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

@Component
class LuckyDrawClock {
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)

    fun now(): LocalDateTime = LocalDateTime.now(clock.withZone(IdolGlowDateTimes.ZONE))

    fun today(): LocalDate = now().toLocalDate()
}
