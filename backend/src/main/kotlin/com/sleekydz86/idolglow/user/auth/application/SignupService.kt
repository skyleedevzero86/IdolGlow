package com.sleekydz86.idolglow.user.auth.application

import com.sleekydz86.idolglow.global.exceptions.CustomException
import com.sleekydz86.idolglow.global.exceptions.UserExceptionType
import com.sleekydz86.idolglow.global.security.JwtProvider
import com.sleekydz86.idolglow.subscription.application.dto.RegisterSubscriptionCommand
import com.sleekydz86.idolglow.subscription.application.port.`in`.SubscriptionPublicUseCase
import com.sleekydz86.idolglow.user.auth.application.dto.TokenResponse
import com.sleekydz86.idolglow.user.user.domain.User
import com.sleekydz86.idolglow.user.user.domain.UserRepository
import com.sleekydz86.idolglow.user.user.domain.vo.Nickname
import com.sleekydz86.idolglow.user.user.domain.vo.UserRole
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class SignupFieldCheckResult(
    val available: Boolean,
    val code: String? = null,
)

@Service
class SignupService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtProvider: JwtProvider,
    private val subscriptionPublicUseCase: SubscriptionPublicUseCase,
) {

    fun checkEmailField(raw: String): SignupFieldCheckResult {
        val t = raw.trim()
        if (t.isBlank()) return SignupFieldCheckResult(false, "BLANK")
        val normalized = normalizeEmail(t) ?: return SignupFieldCheckResult(false, "INVALID_FORMAT")
        if (userRepository.findByEmail(normalized) != null) {
            return SignupFieldCheckResult(false, "TAKEN")
        }
        return SignupFieldCheckResult(true, null)
    }

    fun checkNicknameField(raw: String): SignupFieldCheckResult {
        val t = raw.trim()
        if (t.isBlank()) return SignupFieldCheckResult(false, "BLANK")
        val nickname = try {
            Nickname.of(t)
        } catch (_: CustomException) {
            return SignupFieldCheckResult(false, "INVALID_FORMAT")
        }
        if (userRepository.findByNicknameValue(nickname.value) != null) {
            return SignupFieldCheckResult(false, "TAKEN")
        }
        return SignupFieldCheckResult(true, null)
    }

    @Transactional
    fun signup(
        email: String,
        rawNickname: String,
        password: String,
        subscribeToUpdates: Boolean = false,
    ): TokenResponse {
        val normalizedEmail = normalizeEmail(email)
            ?: throw CustomException(UserExceptionType.INVALID_EMAIL)

        val trimmedPassword = password.trim()
        if (trimmedPassword.isEmpty()) {
            throw CustomException(UserExceptionType.SIGNUP_PASSWORD_REQUIRED)
        }
        validatePasswordStrength(trimmedPassword)

        val nickname = try {
            Nickname.of(rawNickname)
        } catch (e: CustomException) {
            throw e
        }

        if (userRepository.findByEmail(normalizedEmail) != null) {
            throw CustomException(UserExceptionType.EMAIL_ALREADY_REGISTERED)
        }
        if (userRepository.findByNicknameValue(nickname.value) != null) {
            throw CustomException(UserExceptionType.NICKNAME_ALREADY_REGISTERED)
        }

        val encoded = passwordEncoder.encode(trimmedPassword)
        val user = User(
            email = normalizedEmail,
            nickname = nickname,
            profileImageUrl = null,
            passwordHash = encoded,
            role = UserRole.USER,
        )
        val saved = userRepository.save(user)

        if (subscribeToUpdates) {
            subscriptionPublicUseCase.subscribe(
                RegisterSubscriptionCommand(
                    email = normalizedEmail,
                    subscribeNewsletters = true,
                    subscribeIssues = true,
                    source = "SIGNUP",
                )
            )
        }

        saved.updateLastLoginTime()
        return jwtProvider.generateToken(saved.id, saved.role)
    }

    private fun normalizeEmail(raw: String): String? {
        val t = raw.trim().lowercase()
        if (t.isBlank()) return null
        if (!EMAIL_REGEX.matches(t)) return null
        return t
    }

    private fun validatePasswordStrength(password: String) {
        if (password.length < 8 || password.length > 72) {
            throw CustomException(UserExceptionType.SIGNUP_PASSWORD_POLICY)
        }
        val hasLetter = password.any { it.isLetter() }
        val hasDigit = password.any { it.isDigit() }
        if (!hasLetter || !hasDigit) {
            throw CustomException(UserExceptionType.SIGNUP_PASSWORD_POLICY)
        }
    }

    companion object {
        private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")
    }
}
