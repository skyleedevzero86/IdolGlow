package com.sleekydz86.idolglow.platform.user.password

import com.sleekydz86.idolglow.platform.user.port.PlatformPasswordHistoryPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PasswordReuseGuard {

    fun matchesRecentHistory(
        userId: Long,
        rawPassword: String,
        passwordEncoder: PasswordEncoder,
        historyPort: PlatformPasswordHistoryPort,
        lastEntriesToCheck: Int,
    ): Boolean {
        val hashes = historyPort.findRecentEncodedPasswords(userId, lastEntriesToCheck)
        for (hash in hashes) {
            if (passwordEncoder.matches(rawPassword, hash)) {
                return true
            }
        }
        return false
    }
}
