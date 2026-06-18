package com.sleekydz86.idolglow.platform.user.infrastructure.recovery

import java.time.Duration

interface RecoveryJtiStore {
    fun register(
        jti: String,
        ttl: Duration,
    )

    fun consume(jti: String): Boolean
}
