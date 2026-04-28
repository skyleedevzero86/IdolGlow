package com.sleekydz86.idolglow.platform.user.password

import com.sleekydz86.idolglow.platform.user.port.PlatformUserAccountPort
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Service

@Service
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PlatformPasswordValidationService(
    private val policyValidator: PasswordPolicyValidator,
    private val userAccountPort: PlatformUserAccountPort,
) {

    fun validatePassword(password: String): PasswordPolicyValidator.PasswordValidationResult =
        policyValidator.validate(password)

    fun isPasswordChangeRequired(userId: Long): Boolean =
        userAccountPort.findById(userId)
            .map { it.isPasswordChangeRequired() }
            .orElse(false)

    fun isPasswordChangeRecommended(userId: Long): Boolean =
        userAccountPort.findById(userId)
            .map { it.isPasswordChangeRecommended() }
            .orElse(false)

    fun getTodayPasswordChangeCount(userId: Long): Long =
        userAccountPort.findById(userId)
            .map { it.getTodayPasswordChangeCount().toLong() }
            .orElse(0L)
}
