package com.sleekydz86.idolglow.platform.user.infrastructure

import com.sleekydz86.idolglow.platform.user.port.PlatformPasswordHistoryPort
import org.springframework.data.domain.PageRequest
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PlatformPasswordHistoryAdapter(
    private val repository: UserPasswordHistoryJpaRepository,
) : PlatformPasswordHistoryPort {

    override fun findRecentEncodedPasswords(userId: Long, limit: Int): List<String> =
        repository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(0, limit.coerceAtLeast(1)))
            .map { it.passwordHash }

    override fun append(userId: Long, encodedPassword: String) {
        repository.save(
            UserPasswordHistoryEntity(
                userId = userId,
                passwordHash = encodedPassword,
            ),
        )
    }
}
