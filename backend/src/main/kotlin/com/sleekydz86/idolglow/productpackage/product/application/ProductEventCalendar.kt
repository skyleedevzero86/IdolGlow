package com.sleekydz86.idolglow.productpackage.product.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import org.springframework.stereotype.Component
import java.time.Clock
import java.time.LocalDate

@Component
class ProductEventCalendar {
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)

    fun today(): LocalDate = LocalDate.now(clock.withZone(IdolGlowDateTimes.ZONE))
}
