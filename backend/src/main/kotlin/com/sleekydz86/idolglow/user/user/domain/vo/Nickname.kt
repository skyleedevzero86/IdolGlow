package com.sleekydz86.idolglow.user.user.domain.vo

import com.sleekydz86.idolglow.global.exceptions.CustomException
import com.sleekydz86.idolglow.global.exceptions.UserExceptionType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Nickname(
    @Column(name = "nickname", nullable = false, length = 10)
    val value: String
) {

    companion object {
        private val NICKNAME_RULE_PATTERN = Regex("^[a-zA-Z0-9]{2,10}$")

        fun of(raw: String): Nickname {
            val trimmed = raw.trim()
            if (!NICKNAME_RULE_PATTERN.matches(trimmed)) {
                throw CustomException(UserExceptionType.INVALID_NICKNAME)
            }
            return Nickname(trimmed)
        }

        fun defaultFromEmail(email: String): Nickname {
            val localPart = email.substringBefore("@")
            val candidate = localPart.filter { it.isLetterOrDigit() }.take(10)
            return if (NICKNAME_RULE_PATTERN.matches(candidate)) Nickname(candidate) else Nickname("user")
        }
    }
}
