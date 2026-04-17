package com.sleekydz86.idolglow.platform.user.password

import com.sleekydz86.idolglow.platform.auth.config.PlatformAuthProperties
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component
import java.util.regex.Pattern

@Component
@ConditionalOnProperty(prefix = "platform.auth", name = ["enabled"], havingValue = "true", matchIfMissing = true)
class PasswordPolicyValidator(
    private val properties: PlatformAuthProperties,
) {

    data class PasswordValidationResult(
        val valid: Boolean,
        val errors: List<String>,
    )

    fun validate(password: String?): PasswordValidationResult {
        val errors = mutableListOf<String>()
        val rules = properties.password

        if (password.isNullOrEmpty()) {
            errors.add("비밀번호를 입력해 주세요.")
            return PasswordValidationResult(false, errors)
        }

        if (password.length < rules.minLength) {
            errors.add("비밀번호는 최소 ${rules.minLength}자 이상이어야 합니다.")
        }

        if (rules.requireSpecialChars && !SPECIAL_CHARS_PATTERN.matcher(password).find()) {
            errors.add("비밀번호에 특수문자를 최소 1개 포함해야 합니다.")
        }

        if (rules.requireNumbers && !NUMBERS_PATTERN.matcher(password).find()) {
            errors.add("비밀번호에 숫자를 최소 1개 포함해야 합니다.")
        }

        if (rules.requireUppercase && !UPPERCASE_PATTERN.matcher(password).find()) {
            errors.add("비밀번호에 영문 대문자를 최소 1개 포함해야 합니다.")
        }

        if (rules.requireLowercase && !LOWERCASE_PATTERN.matcher(password).find()) {
            errors.add("비밀번호에 영문 소문자를 최소 1개 포함해야 합니다.")
        }

        if (hasConsecutiveCharacters(password)) {
            errors.add("비밀번호에 연속된 문자(예: abc)는 사용할 수 없습니다.")
        }

        if (hasRepeatedCharacters(password)) {
            errors.add("비밀번호에 동일 문자 3회 이상 연속(예: aaa)은 사용할 수 없습니다.")
        }

        if (isCommonWeakPassword(password)) {
            errors.add("너무 흔하거나 취약한 비밀번호입니다.")
        }

        return PasswordValidationResult(errors.isEmpty(), errors)
    }

    private fun hasConsecutiveCharacters(password: String): Boolean {
        for (i in 0 until password.length - 2) {
            val current = password[i]
            val next = password[i + 1]
            val nextNext = password[i + 2]
            if (current.isLetter() && next.isLetter() && nextNext.isLetter()) {
                if (next == current + 1 && nextNext == next + 1) {
                    return true
                }
            }
        }
        return false
    }

    private fun hasRepeatedCharacters(password: String): Boolean {
        for (i in 0 until password.length - 2) {
            val current = password[i]
            val next = password[i + 1]
            val nextNext = password[i + 2]
            if (current == next && next == nextNext) {
                return true
            }
        }
        return false
    }

    private fun isCommonWeakPassword(password: String): Boolean {
        val lowerPassword = password.lowercase()
        val weakPasswords = listOf(
            "password", "123456", "qwerty", "admin", "letmein",
            "welcome", "monkey", "dragon", "master", "hello",
        )
        return weakPasswords.contains(lowerPassword)
    }

    companion object {
        private val SPECIAL_CHARS_PATTERN =
            Pattern.compile("[!@#$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]")
        private val NUMBERS_PATTERN = Pattern.compile("\\d")
        private val UPPERCASE_PATTERN = Pattern.compile("[A-Z]")
        private val LOWERCASE_PATTERN = Pattern.compile("[a-z]")
    }
}
