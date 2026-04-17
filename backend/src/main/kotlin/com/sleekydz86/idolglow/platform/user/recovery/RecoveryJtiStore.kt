package com.sleekydz86.idolglow.platform.user.recovery

import java.time.Duration

interface RecoveryJtiStore {
    fun register(jti: String, ttl: Duration)
    fun consume(jti: String): Boolean
}
