package com.sleekydz86.idolglow.partner.application

import com.sleekydz86.idolglow.global.support.IdolGlowDateTimes
import org.springframework.stereotype.Component
import java.time.Clock
import java.util.concurrent.ConcurrentHashMap

@Component
class PartnerRateLimiter {
    internal var clock: Clock = Clock.system(IdolGlowDateTimes.ZONE)
    private val windows = ConcurrentHashMap<Long, PartnerRateWindow>()

    fun acquire(
        partnerId: Long,
        limitPerMinute: Int,
    ): PartnerRateDecision {
        val nowSeconds = clock.instant().epochSecond
        val minute = nowSeconds / SECONDS_PER_MINUTE
        val window =
            windows.compute(partnerId) { _, current ->
                if (current == null || current.minute != minute) PartnerRateWindow(minute, 1) else current.next()
            }!!
        if (window.count <= limitPerMinute) return PartnerRateDecision(allowed = true, retryAfterSeconds = 0)
        val retryAfter = ((minute + 1) * SECONDS_PER_MINUTE - nowSeconds).coerceAtLeast(1)
        return PartnerRateDecision(allowed = false, retryAfterSeconds = retryAfter)
    }

    private data class PartnerRateWindow(
        val minute: Long,
        val count: Int,
    ) {
        fun next(): PartnerRateWindow = copy(count = count + 1)
    }

    companion object {
        private const val SECONDS_PER_MINUTE: Long = 60
    }
}

data class PartnerRateDecision(
    val allowed: Boolean,
    val retryAfterSeconds: Long,
)
