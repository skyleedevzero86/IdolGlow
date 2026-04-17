package com.sleekydz86.idolglow.platform.user.recovery

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

@Component
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class InMemoryRecoveryJtiStore : RecoveryJtiStore {

    private val expiryMillisByJti = ConcurrentHashMap<String, Long>()

    override fun register(jti: String, ttl: Duration) {
        expiryMillisByJti[jti] = System.currentTimeMillis() + ttl.toMillis()
    }

    override fun consume(jti: String): Boolean {
        val exp = expiryMillisByJti.remove(jti) ?: return false
        return System.currentTimeMillis() <= exp
    }
}
