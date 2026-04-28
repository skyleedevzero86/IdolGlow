package com.sleekydz86.idolglow.airportcrowd.infrastructure

import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
class IncheonAirportApiAuthCooldown {
    private val blockedUntilMillis = ConcurrentHashMap<String, Long>()

    fun isBlocked(apiName: String): Boolean {
        val until = blockedUntilMillis[apiName] ?: return false
        val now = System.currentTimeMillis()
        if (until > now) return true
        blockedUntilMillis.remove(apiName, until)
        return false
    }

    fun markUnauthorized(apiName: String, cooldown: Duration = DEFAULT_COOLDOWN): Boolean {
        val now = System.currentTimeMillis()
        val until = now + cooldown.toMillis()
        val previous = blockedUntilMillis.put(apiName, until)
        return previous == null || previous <= now
    }

    companion object {
        private val DEFAULT_COOLDOWN: Duration = Duration.ofMinutes(5)
    }
}
